package tfg.prod.modules;

import java.util.List;

public class GoogleCredentials {

    private String client_id;
    private String client_secret;
    //private String redirect_uri;//uri a la que se envian las resopuestas de los datos recogidos por la llamada
    private List<String> scopes;

    // getters y setters
    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    /*public String getRedirect_uri() {
        return redirect_uri;
    }

    public void setRedirect_uri(String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }
*/
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}