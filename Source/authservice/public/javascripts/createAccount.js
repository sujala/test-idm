// Globals
var onamevalid = false;
var unamevalid = false;
var pwdvalid = false;
var pwdmatches = false;
var hintvalid = false;

// functions
function toggleRequirements(elem)
{
    var effect = 'slide';
    var options = {};
    var effectmms = 500;

    // hide all requirements
    $("#oname_requirements").hide();
    $("#uname_requirements").hide();
    $("#pwd_requirements").hide();
    $("#pwd_matches").hide();
    $("#hint_requirements").hide();

    switch(elem.name)
    {
        case "oname":
            $("#oname_requirements").toggle(effect, options, effectmms); break;
        case "idname":
            $("#uname_requirements").toggle(effect, options, effectmms); break;
        case "password":
            $("#pwd_requirements").toggle(effect, options, effectmms); break;
        case "confirmPassword":
            $("#pwd_matches").toggle(effect, options, effectmms); break;
        case "passwordHint":
            $("#hint_requirements").toggle(effect, options, effectmms); break;
    }
}

function validateOrganizationName()
{
    var oname = document.createform.oname.value;

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
    toggleONameIcon();
}

function validateIdentityName()
{
    var idname = document.createform.idname.value;
    var url = '/ui/validateUsername?username=' + escape(idname) + '&timestamp=' + new Date().getTime();

    unamevalid = true;
    if (idname.length > 0) {

        if (idname.length < 3)
        {
            setRule('req_idlength', 'failed');
            unamevalid = false;
        }
        else
        {
            setRule('req_idlength', 'passed');
        }

        $.ajax({ url: url,
            type: "GET",
            data: {
                username: $.URLEncode(idname)
            },
            success: function(xml) {
                var xmldoc = loadXmlDoc(xml);
                var identity_exists = getNodeVal(xmldoc, 'identity_exists');

                if (identity_exists == "true" || idname.length < 2) {
                    setRule('req_idnottaken', 'failed');
                    unamevalid = false;
                }
                else {
                    setRule('req_idnottaken', 'passed');
                }

                var identity_char_valid = getNodeVal(xmldoc, 'identity_char_valid');

                if (identity_char_valid == "false") {
                    setRule('req_idvalidchar', 'failed');
                    unamevalid = false;
                }
                else {
                    setRule('req_idvalidchar', 'passed');
                }
                toggleIdNameIcon();
            },
            error: function(xhr) {
                alert('Error!  Status = ' + xhr.status + ": " + xhr.responseText);
            }
        });
    }
    else {
        setRule('req_idnottaken', 'failed');
        setRule('req_idvalidchar', 'failed');
        setRule('req_idlength', 'failed');
        unamevalid = false;
        toggleIdNameIcon();
    }
}

function validatePassword()
{
    var idname = document.createform.idname.value;
    var pwd = document.createform.password.value;

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
    var usernameRegex = new RegExp(idname);
    var no_username = !usernameRegex.test(pwd);

    if (!no_username || pwd.length == 0) {
        setRule('req_pwdnoname', 'failed');
        pwdvalid = false;
    }
    else {
        setRule('req_pwdnoname', 'passed');
    }

    togglePasswordIcon();
    validatePasswordMatches();
}

function validatePasswordMatches()
{
    var password = document.createform.password.value;
    var password2 = document.createform.confirmPassword.value;

    if (password != "" && password == password2) {
        setRule('req_pwdmatches', 'passed');
        pwdmatches = true;
    }
    else {
        setRule('req_pwdmatches', 'failed');
        pwdmatches = false;
    }

    togglePasswordMatchIcon();
}

function validatePasswordHint()
{
    var numchar2compare = 4;

    var password = document.createform.password.value.toLowerCase();
    password = password.replace(/^\s*|\s*$/g,'');

    var hint = document.createform.passwordHint.value.toLowerCase();
    hint = hint.replace(/\s*|\s*/g,'');

    hintvalid = true;

    // hint not empty
    if (hint.length == 0)
    {
        setRule('req_hintsequence', 'failed');
        setRule('req_hintfirstlast', 'failed');
        setRule('req_hintreverse', 'failed');
        setRule('req_hintreplaced', 'failed');
        hintvalid = false;
        toggleHintIcon();
        return;
    }

    if (password.length >= numchar2compare)
    {
        // compare sequence
        var seqvalid = comparePartialPwd(numchar2compare, password, hint);
        if (seqvalid)
        {
            setRule('req_hintsequence', 'passed');
        }
        else
        {
            setRule('req_hintsequence', 'failed');
            hintvalid = false;
        }

        // compare reverse
        var hintrev = strrev(hint);
        var revvalid = comparePartialPwd(numchar2compare, password, hintrev);
        if (revvalid)
        {
            setRule('req_hintreverse', 'passed');
        }
        else
        {
            setRule('req_hintreverse', 'failed');
            hintvalid = false;
        }

        // compare char replaced str
        var replacedhint = hint.replace('@', 'a')
        .replace('3', 'e')
        .replace('1', 'i')
        .replace('!', 'i')
        .replace('0', 'o');

        var replacedvalid = comparePartialPwd(numchar2compare, password, replacedhint);
        if (replacedvalid)
        {
            setRule('req_hintreplaced', 'passed');
        }
        else
        {
            setRule('req_hintreplaced', 'failed');
            hintvalid = false;
        }
    }
    else {
        setRule('req_hintsequence', 'failed');
        setRule('req_hintreverse', 'failed');
        setRule('req_hintreplaced', 'failed');
        hintvalid = false;
    }

    // compare first and last letter
    if (password.length > 0 && hint.length > 0)
    {
        if (password[0] == hint[0] &&
            password[password.length - 1] == hint[hint.length - 1])
        {
            setRule('req_hintfirstlast', 'failed');
            hintvalid = false;
        }
        else
        {
            setRule('req_hintfirstlast', 'passed');
        }
    }

    toggleHintIcon();
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

function toggleONameIcon()
{
    if (onamevalid)
    {
        $('#oname_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#oname_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function toggleIdNameIcon()
{
    if (unamevalid)
    {
        $('#idname_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#idname_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function togglePasswordIcon()
{
    if (pwdvalid)
    {
        $('#password_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#password_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function togglePasswordMatchIcon()
{
    if (pwdmatches)
    {
        $('#confirm_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#confirm_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function toggleHintIcon()
{
    if (hintvalid)
    {
        $('#hint_validated').fadeTo('fast', 1);
    }
    else
    {
        $('#hint_validated').fadeTo('fast', 0.1);
    }
    toggleNextStep();
}

function toggleNextStep()
{
    if (onamevalid && unamevalid && pwdvalid && pwdmatches && hintvalid)
    {
        $('#btn_nextstep').fadeTo('fast', 1.0);
        document.getElementById('btn_nextstep').disabled = false;
    }
    else
    {
        $('#btn_nextstep').fadeTo('fast', 0.1);
        document.getElementById('btn_nextstep').disabled = true;
    }
}

function submitForm()
{
    this.createform.submit();
}