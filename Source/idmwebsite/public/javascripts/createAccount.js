// Globals
var onamevalid = false;
var unamevalid = false;
var pwdvalid = false;
var pwdmatches = false;
var emailvalid = false;
var fullnamevalid = false;
var streetvalid = false;
var cityvalid = false;
var statevalid = false;
var zipvalid = false;

// functions
function toggleRequirements(elem)
{
    var effect = 'slide';
    var options = {};
    var effectmms = 500;

    // hide all requirements
    $("#fullname_requirements").hide();
    $("#uname_requirements").hide();
    $("#pwd_requirements").hide();
    $("#pwd_matches").hide();
    $("#email_requirements").hide();

    $("#oname_requirements").hide();
    $("#street_requirements").hide();
    $("#city_requirements").hide();
    $("#state_requirements").hide();
    $("#zip_requirements").hide();

    switch(elem.name)
    {
        case "username":
            $("#uname_requirements").toggle(effect, options, effectmms); break;
        case "password":
            $("#pwd_requirements").toggle(effect, options, effectmms); break;
        case "confirmPassword":
            $("#pwd_matches").toggle(effect, options, effectmms); break;
        case "fullname":
            $("#fullname_requirements").toggle(effect, options, effectmms); break;
        case "email":
            $("#email_requirements").toggle(effect, options, effectmms); break;
        case "oname":
            $("#oname_requirements").toggle(effect, options, effectmms); break;
        case "street":
            $("#street_requirements").toggle(effect, options, effectmms); break;
        case "city":
            $("#city_requirements").toggle(effect, options, effectmms); break;
        case "state":
            $("#state_requirements").toggle(effect, options, effectmms); break;
        case "zip":
            $("#zip_requirements").toggle(effect, options, effectmms); break;
    }
}

function validateOrganizationName()
{
    var oname = document.signupform.oname.value;

    onamevalid = true;
    if (oname.length < 2)
    {
        setRule('req_onamelength', 'failed');
        onamevalid = false;
    }
    else
    {
        setRule('req_onamelength', 'passed');
    }
    toggleIcon('oname', onamevalid);
}

function validateStreet()
{
    var street = document.signupform.street.value;

    streetvalid = true;
    if (street.length < 1)
    {
        setRule('req_streetlength', 'failed');
        streetvalid = false;
    }
    else
    {
        setRule('req_streetlength', 'passed');
    }
    toggleIcon('street', streetvalid);
}

function validateCity()
{
    var city = document.signupform.city.value;

    cityvalid = true;
    if (city.length < 1)
    {
        setRule('req_citylength', 'failed');
        cityvalid = false;
    }
    else
    {
        setRule('req_citylength', 'passed');
    }
    toggleIcon('city', cityvalid);
}

function validateState()
{
    var state = document.signupform.state.value;

    statevalid = true;
    if (state.length < 1)
    {
        setRule('req_statelength', 'failed');
        statevalid = false;
    }
    else
    {
        setRule('req_statelength', 'passed');
    }
    toggleIcon('state', statevalid);
}

function validateZip()
{
    var zip = document.signupform.zip.value;

    zipvalid = true;
    if (zip.length < 1)
    {
        setRule('req_ziplength', 'failed');
        zipvalid = false;
    }
    else
    {
        setRule('req_ziplength', 'passed');
    }
    toggleIcon('zip', zipvalid);
}

function validateFullName()
{
    var fullname = document.signupform.fullname.value;

    fullnamevalid = true;
    if (fullname.length < 1)
    {
        setRule('req_fullnamelength', 'failed');
        fullnamevalid = false;
    }
    else
    {
        setRule('req_fullnamelength', 'passed');
    }
    toggleIcon('fullname', fullnamevalid);
}

function validateEmail()
{
    var email = document.signupform.email.value;

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

function validateUsername()
{
    var username = document.signupform.username.value;

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
    var username = document.signupform.username.value;
    var pwd = document.signupform.password.value;

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

function validatePasswordMatches()
{
    var password = document.signupform.password.value;
    var password2 = document.signupform.confirmPassword.value;

    if (password != "" && password == password2) {
        setRule('req_pwdmatches', 'passed');
        pwdmatches = true;
    }
    else {
        setRule('req_pwdmatches', 'failed');
        pwdmatches = false;
    }

    toggleIcon('confirmPassword', pwdmatches);
}

function comparePartialPwd(numchar2compare, password, str2compare)
{
    var seqvalid = true;
    var startindex = 0;
    while(startindex + (numchar2compare - 1) <= password.length)
    {
        var pwd_substr = password.substr(startindex, (numchar2compare - 1));
        if (str2compare.indexOf(pwd_substr) > 0)
        {
            seqvalid = false;
        }
        startindex++;
    }

    return seqvalid;
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
    if (fullnamevalid &&
        unamevalid &&
        pwdvalid &&
        pwdmatches &&
        emailvalid && 
        onamevalid &&
        streetvalid &&
        cityvalid &&
        statevalid &&
        zipvalid)
    {
        return true;
    }
    return false;
}

function submitForm()
{
    if (allFieldsValid())
    {
        document.signupform.submit();
    }
}