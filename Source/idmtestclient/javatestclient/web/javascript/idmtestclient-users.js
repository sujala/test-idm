$(document).ready(function(){

    // getUserBtn onClick()
    $('#getUsersBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }

        $(this).attr("disabled", true);
        $('#getUsersStatus').text("");

        var url = API_URL + "/getUsers.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber)
            },
            success: function(resp, textStatus, req) {

                try {
                    var statusCode = $AjaxHelper.getStatusCode(resp);
                    var result = $AjaxHelper.getResult(resp);

                    $('#status_getUsersResponse').text(statusCode);
                    $('#getUsersResponse').val(result);
                }
                catch(err) {
                    alert("Error setting response.");
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_getUsersResponse').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getUsersBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    //addFirstUserBtn onClick()
    $('#addFirstUserBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();
        var username = $('#users_username').val();
        var password = $('#users_password').val();
        var firstName = $('#users_firstname').val();
        var lastName = $('#users_lastname').val();
        var middleName = $('#users_middlename').val();
        var email = $('#users_email').val();
        var secretQuestion = $('#users_secretQuestion').val();
        var secretAnswer = $('#users_secretAnswer').val();

        if (username == "") {
            alert("Please enter a username.");
            return;
        }
        if (password == "") {
            alert("Please enter a password.");
            return;
        }

        $(this).attr("disabled", true);
        $('#status_users_response').text("");

        var url = API_URL + "/addFirstUser.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber),
                username: $.URLEncode(username),
                password: $.URLEncode(password),
                firstName: $.URLEncode(firstName),
                lastName: $.URLEncode(lastName),
                middleName: $.URLEncode(middleName),
                email: $.URLEncode(email),
                secretQuestion: $.URLEncode(secretQuestion),
                secretAnswer: $.URLEncode(secretAnswer)
            },
            success: function(resp) {
                try {
                    var statusCode = $AjaxHelper.getStatusCode(resp);
                    var result = $AjaxHelper.getResult(resp);

                    $('#status_users_response').text(statusCode);
                    $('#users_response').val(result);
                }
                catch(err) {
                    alert("Error setting response.")
                }
            },
            error: function(req, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_getUsersResponse').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#addFirstUserBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    //addUserBtn onClick()
    $('#addUserBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();
        var username = $('#users_username').val();
        var password = $('#users_password').val();
        var firstName = $('#users_firstname').val();
        var lastName = $('#users_lastname').val();
        var middleName = $('#users_middlename').val();
        var email = $('#users_email').val();
        var secretQuestion = $('#users_secretQuestion').val();
        var secretAnswer = $('#users_secretAnswer').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }
        if (username == "") {
            alert("Please enter a username.");
            return;
        }
        if (password == "") {
            alert("Please enter a password.");
            return;
        }

        $('#status_users_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/addUser.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber),
                username: $.URLEncode(username),
                password: $.URLEncode(password),
                firstName: $.URLEncode(firstName),
                lastName: $.URLEncode(lastName),
                middleName: $.URLEncode(middleName),
                email: $.URLEncode(email),
                secretQuestion: $.URLEncode(secretQuestion),
                secretAnswer: $.URLEncode(secretAnswer)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_users_response').text(statusCode);
                $('#users_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_users_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#addUserBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    //getUserBtn onClick()
    $('#getUserBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();
        var username = $('#users_username').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }
        if (username == "") {
            alert("Please enter a username.");
            return;
        }

        $('#status_users_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/getUser.jsp";
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

                $('#status_users_response').text(statusCode);
                $('#users_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_users_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#getUserBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    //updateUserBtn onClick()
    $('#updateUserBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();
        var username = $('#users_username').val();
        var firstName = $('#users_firstname').val();
        var lastName = $('#users_lastname').val();
        var middleName = $('#users_middlename').val();
        var email = $('#users_email').val();
        var secretQuestion = $('#users_secretQuestion').val();
        var secretAnswer = $('#users_secretAnswer').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }
        if (username == "") {
            alert("Please enter a username.");
            return;
        }

        $('#status_users_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/updateUser.jsp";
        $.ajax({
            type: 'POST',
            url: url,
            data: {
                accessToken: $.URLEncode(accessToken),
                customerNumber: $.URLEncode(customerNumber),
                username: $.URLEncode(username),
                firstName: $.URLEncode(firstName),
                lastName: $.URLEncode(lastName),
                middleName: $.URLEncode(middleName),
                email: $.URLEncode(email),
                secretQuestion: $.URLEncode(secretQuestion),
                secretAnswer: $.URLEncode(secretAnswer)
            },
            success: function(resp) {
                var statusCode = $AjaxHelper.getStatusCode(resp);
                var result = $AjaxHelper.getResult(resp);

                $('#status_users_response').text(statusCode);
                $('#users_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_users_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#updateUserBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    //deleteUserBtn onClick()
    $('#deleteUserBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();
        var username = $('#users_username').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }
        if (username == "") {
            alert("Please enter a username.");
            return;
        }

        $('#status_users_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/deleteUser.jsp";
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

                $('#status_users_response').text(statusCode);
                $('#users_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_users_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#deleteUserBtn').attr("disabled", false);
                }, 500);
            }
        });
    });

    //resetUserApiKeyBtn onClick()
    $('#resetUserApiKeyBtn').click(function() {
        var accessToken = $('#access_token').val();
        var customerNumber = $('#users_customerNumber').val();
        var username = $('#users_username').val();

        if (customerNumber == "") {
            alert("Please enter a customer number.");
            return;
        }
        if (username == "") {
            alert("Please enter a username.");
            return;
        }

        $('#status_users_response').text("");
        $(this).attr("disabled", true);

        var url = API_URL + "/resetUserApiKey.jsp";
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

                $('#status_users_response').text(statusCode);
                $('#users_response').val(result);
            },
            error: function(response, textStatus) {
                alert("error: " + req.status + " : " + textStatus);
                $('#status_users_response').text(req.status);
            },
            complete: function() {
                setTimeout(function() {
                    $('#resetUserApiKeyBtn').attr("disabled", false);
                }, 500);
            }
        });
    });
});