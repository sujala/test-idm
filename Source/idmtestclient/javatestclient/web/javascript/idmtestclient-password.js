$(document).ready(function(){

    // getPasswordRulesBtn onClick()
    $('#getPasswordRulesBtn').click(function() {
        var accessToken = $('#access_token').val();

        $('#status_password_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/getPasswordRules.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_password_response').text(statusCode);
                $('#password_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_password_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getPasswordRulesBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // validatePasswordBtn onClick()
    $('#validatePasswordBtn').click(function() {
        var accessToken = $('#access_token').val();
        var password = $('#password_password').val();

        $('#status_password_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/validatePassword.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                passwordToValidate: $.URLEncode(password)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_password_response').text(statusCode);
                $('#password_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_password_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#validatePasswordBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // getPasswordBtn onClick()
    $('#getPasswordBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#password_customerNumber').val();
        var username = $('#password_username').val();

        $('#status_userpassword').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/getUserPassword.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber),
                username: $.URLEncode(username)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_userpassword').text(statusCode);
                $('#password_userpassword_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_userpassword').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getPasswordBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // setPasswordBtn onClick()
    $('#setPasswordBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#password_customerNumber').val();
        var username = $('#password_username').val();
        var oldPassword = $('#password_userpassword').val();
        var newPassword = $('#password_usernewpassword').val();

        $('#status_userpassword').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/setUserPassword.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber),
                username: $.URLEncode(username),
                oldPassword: $.URLEncode(oldPassword),
                newPassword: $.URLEncode(newPassword)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_userpassword').text(statusCode);
                $('#password_userpassword_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_userpassword').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#setPasswordBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    // resetPasswordBtn onClick()
    $('#resetPasswordBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#password_customerNumber').val();
        var username = $('#password_username').val();

        $(this).attr("disabled", true);

        var url = API_URL + "/resetUserPassword.jsp";

        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber),
                username: $.URLEncode(username)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_userpassword').text(statusCode);
                $('#password_userpassword_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_userpassword').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#resetPasswordBtn').attr("disabled", false);
                }, 500);
            }
        });
    });
});