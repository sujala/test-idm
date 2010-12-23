// Globals

var unamevalid = false;
var pwdvalid = false;
var firstnamevalid = false;
var lastnamevalid = false;
var emailvalid = false;
var secretquestionvalid = false;
var secretanswervalid = false;

// functions
function toggleRequirements(elem)
{
    var effect = 'slide';
    var options = {};
    var effectmms = 500;

    // hide all requirements
    $("#uname_requirements").hide();
    $("#pwd_requirements").hide();
    $("#firstname_requirements").hide();
    $("#lastname_requirements").hide();
    $("#middlename_requirements").hide();
    $("#email_requirements").hide();
    $("#secretquestion_requirements").hide();
    $("#secretanswer_requirements").hide();

    switch(elem.name)
    {
        case "username":
            $("#uname_requirements").toggle(effect, options, effectmms); break;
        case "password":
            $("#pwd_requirements").toggle(effect, options, effectmms); break;
        case "firstname":
            $("#firstname_requirements").toggle(effect, options, effectmms); break;
        case "lastname":
            $("#lastname_requirements").toggle(effect, options, effectmms); break;
        case "middlename":
            $("#middlename_requirements").toggle(effect, options, effectmms); break;
        case "email":
            $("#email_requirements").toggle(effect, options, effectmms); break;
        case "secretquestion":
            $("#secretquestion_requirements").toggle(effect, options, effectmms); break;
        case "secretanswer":
            $("#secretanswer_requirements").toggle(effect, options, effectmms); break;
    }
}

function validateUsername()
{
    var username = document.addadminform.username.value;

    unamevalid = true;
    if (username.length > 0) {

        if (username.length < 3)
        {
            setRule('req_idlength', 'failed');
            unamevalid = false;
        }
        else
        {
            setRule('req_idlength', 'passed');
        }

        var charRegex = new RegExp(/[^A-Za-z0-9\._-]/);
        if (charRegex.test(username)) {
            setRule('req_idvalidchar', 'failed');
            unamevalid = false;
        }
        else {
            setRule('req_idvalidchar', 'passed');
        }

        setRule('req_idnottaken', 'passed'); // todo
        toggleIcon('username', unamevalid);
    }
    else {
        setRule('req_idvalidchar', 'failed');
        setRule('req_idlength', 'failed');
        setRule('req_idnottaken', 'failed');
        unamevalid = false;
        toggleIcon('username', unamevalid);
    }
}

function validatePassword()
{
    var username = document.addadminform.username.value;
    var pwd = document.addadminform.password.value;

    pwdvalid = true;

    // password length
    if (pwd.length < 7 )
    {
        setRule('req_pwdlength', 'failed');
        pwdvalid = false;
    }
    else {
        setRule('req_pwdlength', 'passed');
    }

    // valid characters set
    var upperRegex = new RegExp(/[A-Z]/);
    var lowerRegex = new RegExp(/[a-z]/);
    var numberRegex = new RegExp(/\d/);
    var symbolRegex = new RegExp(/\W/);

    var char_valid = (upperRegex.test(pwd) && lowerRegex.test(pwd)
        && numberRegex.test(pwd) && symbolRegex.test(pwd));

    if (!char_valid) {
        setRule('req_pwdvalidchar', 'failed');
        pwdvalid = false;
    }
    else {
        setRule('req_pwdvalidchar', 'passed');
    }

    // not a forbidden word
    var pwd2 = jQuery.trim(pwd.replace("@", "a").replace("0", "o").replace(" ", ""));
    var wordRegex = new RegExp(/password/i);
    var word_valid = !wordRegex.test(pwd2);
    if (!word_valid || pwd.length == 0) {
        setRule('req_pwdvalidword', 'failed');
        pwdvalid = false;
    }
    else {
        setRule('req_pwdvalidword', 'passed');
    }

    // doesn't have repeating char
    var no_sequence = true;
    for(var i=0; i<pwd.length; i++)
    {
        var curChar = pwd[i];
        var numCount = 1;
        var numCharToCheck = 2;

        for (var j=i+1; j<pwd.length;j++) {
            if (pwd[j] == curChar) {
                numCount++;
            }
            if (numCharToCheck == 0) {
                break;
            }
        }
        if (numCount >= 3) {
            no_sequence = false;
            break;
        }
    }
    if (!no_sequence || pwd.length == 0) {
        setRule('req_pwdnosequence', 'failed');
        pwdvalid = false;
    }
    else {
        setRule('req_pwdnosequence', 'passed');
    }

    // username not in password
    var usernameRegex = new RegExp(username);
    var no_username = !usernameRegex.test(pwd);

    if (!no_username || pwd.length == 0) {
        setRule('req_pwdnoname', 'failed');
        pwdvalid = false;
    }
    else {
        setRule('req_pwdnoname', 'passed');
    }

    toggleIcon('password', pwdvalid);
    validatePasswordMatches();
}

function validateFirstname()
{
    var firstname = document.addadminform.firstname.value;

    firstnamevalid = true;
    if (firstname.length < 1)
    {
        setRule('req_firstnamelength', 'failed');
        firstnamevalid = false;
    }
    else
    {
        setRule('req_firstnamelength', 'passed');
    }
    toggleIcon('firstname', firstnamevalid);
}

function validateLastname()
{
    var lastname = document.addadminform.lastname.value;

    lastnamevalid = true;
    if (lastname.length < 1)
    {
        setRule('req_lastnamelength', 'failed');
        lastnamevalid = false;
    }
    else
    {
        setRule('req_lastnamelength', 'passed');
    }
    toggleIcon('lastname', lastnamevalid);
}

function validateEmail()
{
    var email = document.addadminform.email.value;

    var emailRegex = new RegExp( /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/);
    if (emailRegex.test(email)) {
        setRule('req_emailvalid', 'passed');
        emailvalid = true;
    }
    else {
        setRule('req_emailvalid', 'failed');
        emailvalid = false;
    }

    toggleIcon('email', emailvalid);
}

function validateSecretQuestion()
{
    var secretquestion = document.addadminform.secretquestion.value;

    secretquestionvalid = true;
    if (secretquestion.length < 1)
    {
        setRule('req_secretquestionlength', 'failed');
        secretquestionvalid = false;
    }
    else
    {
        setRule('req_secretquestionlength', 'passed');
    }
    toggleIcon('secretquestion', secretquestionvalid);
}

function validateSecretAnswer()
{
    var secretanswer = document.addadminform.secretanswer.value;

    secretanswervalid = true;
    if (secretanswer.length < 1)
    {
        setRule('req_secretanswerlength', 'failed');
        secretanswervalid = false;
    }
    else
    {
        setRule('req_secretanswerlength', 'passed');
    }
    toggleIcon('secretanswer', secretanswervalid);
}

function loadXmlDoc(text)
{
    if (window.DOMParser)
    {
        parser=new DOMParser();
        xmlDoc=parser.parseFromString(text,"text/xml");
    }
    else // Internet Explorer
    {
        xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
        xmlDoc.async="false";
        xmlDoc.loadXML(text);
    }
    return xmlDoc;
}

function getNodeVal(xmldoc, nodename)
{
    if (xmldoc.getElementsByTagName(nodename)[0]){
        return xmldoc.getElementsByTagName(nodename)[0].firstChild.nodeValue;
    }
    else {
        return 0;
    }
}

function setRule(id, passfail)
{
    if (passfail == 'passed')
    {
        $('#' +id).attr("class","requirement_bullet_green");
    }
    else
    {
        $('#' +id).attr("class","requirement_bullet_gray");
    }
}

function strrev(str) {
  return str.split("").reverse().join("");
}

function toggleIcon(fieldName, fieldValid)
{
    if (fieldValid)
    {
        $('#' + fieldName + '_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#' + fieldName + '_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function toggleNextStep()
{
    if (allFieldsValid())
    {
        $('#btn_nextstep').fadeTo('fast', 1.0);
    }
    else
    {
        $('#btn_nextstep').fadeTo('fast', 0.2);
    }
}

function allFieldsValid()
{
    if (unamevalid &&
        pwdvalid && 
        firstnamevalid &&
        lastnamevalid &&
        emailvalid &&
        secretquestionvalid &&
        secretanswervalid)
    {
        return true;
    }
    return false;
}

function submitForm()
{
    if (allFieldsValid())
    {
        document.addadminform.submit();
    }
}