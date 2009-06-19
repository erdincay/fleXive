package com.flexive.tools;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command-line utility to create a "flat" storage schema for content properties.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class FlatStorageCreator {
    public static enum ColumnType {
        STRING, TEXT, BIGINT, DOUBLE, SELECT
    }

    private static final class ParseTemplateException extends Exception {
        private static final long serialVersionUID = 458498439976430343L;

        private ParseTemplateException(String message) {
            super(message);
        }
    }

    @SuppressWarnings({"serial"})
    /** Default column counts per type */
    private static final Map<ColumnType, Integer> DEFAULT_CONFIGURATION = Collections.unmodifiableMap(new HashMap<ColumnType, Integer>() {
        {
            put(ColumnType.STRING, 20);
            put(ColumnType.TEXT, 10);
            put(ColumnType.BIGINT, 10);
            put(ColumnType.DOUBLE, 10);
            put(ColumnType.SELECT, 10);
        }
    });

    private final Connection connection;
    private final String tableName;
    private final String schema;
    private final boolean overwrite;
    private final Map<ColumnType, Integer> columnCounts;

    public FlatStorageCreator(Connection connection, String tableName, String schema, boolean overwrite, Map<ColumnType, Integer> overrideColumnCounts) {
        this.connection = connection;
        this.tableName = tableName.toUpperCase();
        this.schema = schema.toUpperCase();
        this.overwrite = overwrite;
        this.columnCounts = new HashMap<ColumnType, Integer>(DEFAULT_CONFIGURATION);
        // override default column counts
        if (overrideColumnCounts != null) {
            for (Map.Entry<ColumnType, Integer> entry : overrideColumnCounts.entrySet()) {
                this.columnCounts.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Create the flat storage table structure.
     *
     * @throws SQLException
     * @throws IOException
     */
    public void createTable() throws SQLException, IOException {
        final String sql = processTemplate("create-template.sql");

        // split template into single statements and execute them as a batch
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            for (String query : sql.split(";")) {
                if (query.trim().length() > 0) {
                    stmt.addBatch(query);
                }
            }
            stmt.executeBatch();

            // unless the container commits 
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage() + "\nQuery was:\n" + sql, e);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private String processTemplate(String filename) throws SQLException, IOException {
        final String template = getTemplate(filename);
        try {
            return expandTemplate(template);
        } catch (ParseTemplateException e) {
            throw new IllegalArgumentException("Failed to parse template: " + e.getMessage());
        }
    }

    private String expandTemplate(String template) throws ParseTemplateException {
        // process _TIMES loops to create the columns
        final Pattern timesPattern = Pattern.compile("\\$\\{([A-Z]+)_TIMES\\} *");
        final StringBuilder expanded = new StringBuilder();
        final String overwriteSwitch = "${OVERWRITE}";
        for (String line : template.split("\n")) {
            final Matcher matcher = timesPattern.matcher(line);
            if (matcher.find()) {
                // ${..._TIMES} matched, get column name
                final String type = matcher.group(1);   // STRING, INT, ...
                final int count;
                try {
                    final ColumnType columnType = ColumnType.valueOf(type);
                    count = columnCounts.containsKey(columnType) ? columnCounts.get(columnType) : 1;
                } catch (IllegalArgumentException e) {
                    throw new ParseTemplateException("Invalid type argument for _TIMES: " + type);
                }

                final String head = line.substring(0, matcher.start());
                final String tail = line.substring(matcher.end());
                for (int i = 0; i < count; i++) {
                    // add content of the line so far
                    expanded.append(head);
                    // add interpolated line content
                    expanded.append(tail.replace("${INDEX}", String.valueOf(i)));
                    expanded.append("\n");
                }
            } else if (line.indexOf(overwriteSwitch) != -1) {
                // write only if overwrite is enabled
                if (overwrite) {
                    final int start = line.indexOf(overwriteSwitch) + overwriteSwitch.length();
                    expanded.append(line.substring(start).trim()).append("\n");
                }
            } else {
                // passthrough
                expanded.append(line).append("\n");
            }
        }

        // replace global vars
        return expanded.toString().replace("${TABLE_NAME}", tableName).replace("${SCHEMA}", schema);
    }

    private String getTemplate(String filename) throws SQLException, IOException {
        final String dbVendor = connection.getMetaData().getDatabaseProductName().toLowerCase();
        final InputStream is = Thread.currentThread().getContextClassLoader()
                .getResource(dbVendor + "/" + filename)
                .openStream();
        return IOUtils.toString(is);
    }

    public static void main(String[] args) {
        final Options options = new Options();
        options.addOption("o", "overwrite", false, "overwrite existing table");
        options.addOption("u", "user", true, "the database user name");
        options.addOption("p", "password", true, "the database user password");
        options.addOption("ns", "nstring", true, "number of STRING columns");
        options.addOption("nt", "ntext", true, "number of TEXT columns");
        options.addOption("ni", "nbigint", true, "number of BIGINT columns");
        options.addOption("nd", "ndouble", true, "number of DOUBLE columns");
        options.addOption("nsel", "nselect", true, "number of SELECT columns");
        final CommandLine commandLine;
        try {
            commandLine = new PosixParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            usage(options);
            return;
        }
        if (commandLine.getArgs().length != 3 || !commandLine.hasOption("user")) {
            usage(options);
            return;
        }

        final String url = commandLine.getArgs()[0];
        final String schema = commandLine.getArgs()[1];
        final String tableName = commandLine.getArgs()[2];

        try {
            // load supported database drivers
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Class.forName("org.h2.Driver").newInstance();
        } catch (Exception e) {
            System.err.println("Failed to initialize the database drivers: " + e.getMessage());
            return;
        }

        Connection con = null;
        try {
            final Map<ColumnType, Integer> overrides = new HashMap<ColumnType, Integer>();
            for (ColumnType type : ColumnType.values()) {
                final String argName = "n" + type.name().toLowerCase();
                if (commandLine.hasOption(argName)) {
                    try {
                        overrides.put(type, Integer.valueOf(commandLine.getOptionValue(argName)));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid value for argument " + argName + ": " + commandLine.getOptionValue(argName));
                        return;
                    }
                }
            }
            con = DriverManager.getConnection(
                    url,
                    commandLine.getOptionValue("user"),
                    commandLine.getOptionValue("password")
            );
            final FlatStorageCreator fsc = new FlatStorageCreator(
                    con,
                    tableName,
                    schema,
                    commandLine.hasOption("overwrite"),
                    overrides
            );
            fsc.createTable();
        } catch (SQLException e) {
            System.err.println("Database error:" + e.getMessage() + "(" + e.getClass().getCanonicalName() + ")");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Failed to load template: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e);
                }
            }
        }

    }

    private static void usage(Options options) {
        new HelpFormatter().printHelp(
                "java -jar flatStorageCreator.jar [options] jdbc-connection-url schema tableName\n",
                options
        );
    }
}
