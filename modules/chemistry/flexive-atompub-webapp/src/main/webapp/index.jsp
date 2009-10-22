<html>
<head>
    <title>[fleXive] CMIS AtomPub interface</title>
</head>
<body>

    <h2>[fleXive] CMIS AtomPub interface</h2>

    <h3>Repository URL</h3>

    <p>
        <strong>
            <a href="cmis/repository"><%=
            "http" +(request.isSecure() ? "s" : "") + "://"
                    + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath() 
                    + "/cmis/repository"%></a>
        </strong>
    </p>

</body>
</html>
