var API_URL = "http://localhost:8080/IdmTestClientJava/jsp-scripts/";

var tokenFormVals = {
    "admin": {
            "clientId": "ABCDEF",
            "clientSecret": "password",
            "username": "mkovacs",
            "password": "password"
    },
    "app-billing": {
            "clientId": "GHIJKL",
            "clientSecret": "password",
            "username": "",
            "password": ""
    },
    "app-cloudcp": {
            "clientId": "ABCDEF",
            "clientSecret": "password",
            "username": "",
            "password": ""
    },
    "signup-service": {
            "clientId": "MNOPQR",
            "clientSecret": "password",
            "username": "",
            "password": ""
    },
    "racker": {
            "clientId": "ABCDEF",
            "clientSecret": "password",
            "username": "john.eo",
            "password": "password"
    },
    "non-admin": {
            "clientId": "ABCDEF",
            "clientSecret": "password",
            "username": "huey",
            "password": "P@ssw0rd"
    },
    "refresh-token": {
            "clientId": "ABCDEF",
            "clientSecret": "password",
            "username": "",
            "password": ""
    }
};

$AjaxHelper = new function() {
    this.getStatusCode = function(resp) {
        var statusCode = $(resp).find("statusCode").text();
        return statusCode;
    },
    this.getResult = function (resp) {
        var result = $(resp).find("result").text();
        result = $.URLDecode(result);
        result = result.replace(/\+/g, " ");
        return result;
    };
}