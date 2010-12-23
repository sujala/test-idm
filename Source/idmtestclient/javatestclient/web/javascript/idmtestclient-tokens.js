$(document).ready(function(){

    // page init
    $('#getTokenBtn').val("getToken");
    $('#getTokenBtn').attr("disabled", false);

    // tokenType onChange()
    $('#tokenType').change(function() {
        var tokenType = $('#tokenType option:selected').val();

        var clientId = "";
        var clientSecret = "";
        var username = "";
        var password = "";

        if (tokenType != "") {
            clientId = tokenFormVals[tokenType].clientId;
            clientSecret = tokenFormVals[tokenType].clientSecret;
            username = tokenFormVals[tokenType].username;
            password = tokenFormVals[tokenType].password;
        }

        $('#clientId').val(clientId);
        $('#clientSecret').val(clientSecret);
        $('#username').val(username);
        $('#password').val(password);
    });

    // getTokenBtn onClick()
    $('#getTokenBtn').click(function() {

        $('#getTokenErrorMsg').text("");

        var tokenType = $('#tokenType option:selected').val();
        var clientId = $('#clientId').val();
        var clientSecret = $('#clientSecret').val();
        var username = $('#username').val();
        var password = $('#password').val();
        var refreshToken = $('#refreshToken').val();

        if (tokenType != "refresh-token") {
            if (clientId == "" || clientSecret == "") {
                alert("Please enter the clientId and clientSecret.");
                return;
            }
        }

        $(this).val("calling ...");
        $(this).attr("disabled", true);

        var url = API_URL + "/getToken.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                "tokenType": $.URLEncode(tokenType),
                "clientId": $.URLEncode(clientId),
                "clientSecret": $.URLEncode(clientSecret),
                "username": $.URLEncode(username),
                "password": $.URLEncode(password),
                "refreshToken": $.URLEncode(refreshToken)
            },
            success: function(resp) {
                try {
                    var result = $AjaxHelper.getResult(resp);
                    var tokenId = $(result).find("access_token").attr("id");
                    var refreshToken = $(result).find("refresh_token").attr("id");

                    if(tokenId == "" || typeof(tokenId) == "undefined") {
                        $('#getTokenErrorMsg').text("Authorization failed");
                    }

                    $('#access_token').val(tokenId);
                    $('#tokens_tokenId').val(tokenId);
                    $('#refreshToken').val(refreshToken);
                }
                catch(err) {
                    alert("Error encountered: " + err);
                    $('#access_token').val("");
                }
                setTimeout(function() {
                    $('#getTokenBtn').val("getToken");
                    $('#getTokenBtn').attr("disabled", false);
                }, 500);
            },
            error: function(xhr, ajaxOptions, thrownError){
                    alert("Error: " + xhr.status);
            }
        });
    });

    // validateTokenBtn onClick()
    $('#validateTokenBtn').click(function() {

        var accessToken = $('#access_token').val();
        var tokenToValidate = $('#tokens_tokenId').val();

        if (tokenToValidate == "") {
            alert("Please enter the token to validate.");
            return;
        }

        $('#status_tokens_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/validateToken.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                "accessToken": $.URLEncode(accessToken),
                "tokenToValidate": $.URLEncode(tokenToValidate)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_tokens_response').text(statusCode);
                $('#tokens_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_tokens_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#validateTokenBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // revokeTokenBtn onClick()
    $('#revokeTokenBtn').click(function() {

        var accessToken = $('#access_token').val();
        var tokenToRevoke = $('#tokens_tokenId').val();

        if (tokenToRevoke == "") {
            alert("Please enter the token to revoke.");
            return;
        }

        $(this).attr("disabled", true);

        var url = API_URL + "/revokeToken.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                "accessToken": $.URLEncode(accessToken),
                "tokenToRevoke": $.URLEncode(tokenToRevoke)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_tokens_response').text(statusCode);
                $('#tokens_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_tokens_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#revokeTokenBtn').attr("disabled", false);
                }, 500);
            }
        });
    });
});