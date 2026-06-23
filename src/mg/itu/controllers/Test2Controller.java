package mg.itu.controllers;

import mg.itu.annotation.MonController;
import mg.itu.annotation.MaFonction;

@MonController(url = "/test2")
public class Test2Controller {

    public void creerItem() {}

    @MaFonction
    public void supprimerItem() {}
}
