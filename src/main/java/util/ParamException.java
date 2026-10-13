package util;

// Valeur envoyée par le formulaire impossible à convertir (renvoie une erreur 400)
public class ParamException extends RuntimeException {

    public ParamException(String message) {
        super(message);
    }

}
