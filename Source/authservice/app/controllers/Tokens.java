package controllers;
import org.apache.commons.lang.StringUtils;

import library.TokenService;
import play.data.validation.Required;
import play.mvc.*;

@With(Security.class)
public class Tokens extends Controller {

    public static void index() {
            render();
    }

    public static void validateToken(@Required String token, @Required String username)
    {   
        if (StringUtils.isBlank(username)) {
                error(400, "Missing required parameter: username");
        }

        TokenService tokenService = new TokenService();
        Boolean tokenValid = tokenService.ValidateToken(token, username);
        
        if (!tokenValid) 
        {
            error(401, "Invalid or expired token.");
        }
        
        render(tokenValid);
    }
}