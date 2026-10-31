# Mini-framework MVC en Java

Un mini-framework MVC maison, construit sur l'API Servlet (Jakarta Servlet 6) et inspiré de Spring MVC.
Une seule servlet (`FrontControllerServlet`) reçoit toutes les requêtes et les envoie vers la bonne méthode
d'un contrôleur, repérée grâce à des annotations.

## Prérequis

- Java 17
- Tomcat 10 ou plus (API `jakarta.*`)
- Maven (le projet contient `mvnw`)

Le framework n'a aucune dépendance en plus de l'API Servlet, que Tomcat fournit.

## Installation

1. Construire le jar : `./mvnw clean package` (ou `mvn clean package`). Le jar est dans `target/`.
2. Le copier dans le dossier `WEB-INF/lib/` de l'application web.
3. Déclarer la servlet dans le `web.xml` de l'application :

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">

    <!-- packages à scanner, sous-packages compris ; plusieurs possibles : app.controllers;app.api -->
    <context-param>
        <param-name>controller-package</param-name>
        <param-value>app.controllers</param-value>
    </context-param>

    <!-- vue "page" = /WEB-INF/views/page.jsp -->
    <context-param>
        <param-name>view-prefix</param-name>
        <param-value>/WEB-INF/views/</param-value>
    </context-param>
    <context-param>
        <param-name>view-suffix</param-name>
        <param-value>.jsp</param-value>
    </context-param>

    <servlet>
        <servlet-name>front-controller</servlet-name>
        <servlet-class>controller.FrontControllerServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>front-controller</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```

Le listener du framework (`AppContextListener`) s'enregistre tout seul grâce à `@WebListener` : il n'y a rien
d'autre à déclarer. Si le `web.xml` contient `metadata-complete="true"`, il faut alors ajouter
`<listener><listener-class>listener.AppContextListener</listener-class></listener>`.

Utiliser `<url-pattern>/</url-pattern>` et non `/*` : avec `/*`, les vues JSP (forward) repasseraient par le framework.

### Paramètres du `web.xml`

| Paramètre (`context-param`) | Obligatoire | Rôle |
|---|---|---|
| `controller-package` | oui | package(s) des contrôleurs, séparés par `;` ou `,` |
| `view-prefix`, `view-suffix` | pour les vues JSP | début et fin du chemin d'une vue |
| `auth-session-key` | non (`utilisateur`) | nom de la donnée de session qui représente l'utilisateur connecté |
| `role-session-key` | non (`role`) | nom de la donnée de session qui contient le ou les rôles |
| `login-url` | non | page de connexion vers laquelle on redirige un visiteur non connecté |

## Utilisation

### Contrôleur et routes

```java
@Annotation                                   // marque la classe comme contrôleur
public class AccueilController {

    @UrlAnnotation("/hello")                  // GET par défaut
    public String hello() { return "Bonjour"; }

    @UrlAnnotation(value = "/contact", httpmethode = HTTPmethode.POST)
    public String contact() { return "Reçu en POST"; }
}
```

Seuls `GET` et `POST` sont gérés. Deux méthodes ne peuvent pas avoir la même URL avec la même méthode HTTP :
l'application refuse alors de démarrer (« route en double »).

**Un contrôleur n'est créé qu'une seule fois** : à la première requête qui l'utilise (rien n'est créé au démarrage),
puis la même instance sert toutes les routes de la classe, pour tous les utilisateurs. La classe doit avoir un
constructeur sans argument ; s'il échoue, l'erreur est renvoyée et la requête suivante réessaie.

> **Attention :** les attributs d'instance d'un contrôleur sont **partagés entre tous les utilisateurs et toutes les
> requêtes simultanées**. N'y range pas de données propres à un utilisateur (un `private String nom;` serait écrasé
> par la requête suivante) : utilise la `Session`, ou des variables locales de la méthode.

### Paramètres d'une méthode

Les champs d'un formulaire (ou de l'URL) sont injectés dans les paramètres. Chaque paramètre est relié à son champ
par `@Param` :

```java
@UrlAnnotation(value = "/etudiant/save", httpmethode = HTTPmethode.POST)
public String save(@Param("nom") String nom, @Param("age") int age, @Param("boursier") boolean boursier) { ... }
```

- **Types simples** : `String`, `int`, `long`, `double`, `float`, `short`, `byte`, `char`, `boolean` et leurs classes
  (`Integer`...). Champ absent ou vide : `0`, `false` ou `null`. Valeur invalide (`age=abc`) : erreur **400**.
- **Objets** : les champs sont préfixés par le nom du paramètre (`personne.nom`, `personne.adresse.ville`).
  La classe doit avoir un constructeur sans argument ; le setter est utilisé s'il existe, sinon l'attribut est rempli
  directement.
  ```java
  public String save(@Param("personne") Personne personne) { ... }
  ```
- **Fournis par le framework** (sans `@Param`) : `Session`, `ServletContext`, `HttpServletRequest`, `HttpServletResponse`.
- Sans `@Param`, le nom Java du paramètre est utilisé, mais seulement si le code est compilé avec `-parameters`.

### Vues JSP : `ModelAndView`

```java
@UrlAnnotation("/etudiant")
public ModelAndView etudiant() {
    ModelAndView mav = new ModelAndView("etudiant");     // /WEB-INF/views/etudiant.jsp
    mav.set("etudiant", new Etudiant("Rakoto", 21));     // n'importe quel type de valeur
    return mav;
}
```

Dans la JSP : `${etudiant.nom}`. Autres méthodes : `get(nom)`, `remove(nom)`, `setUrlSuivant(vue)`, `setList(map)`.

### JSON : `@RestAPI`

La valeur retournée (texte, nombre, objet, liste, `Map`...) est envoyée en JSON.

```java
@RestAPI
@UrlAnnotation("/api/etudiant")
public Etudiant etudiant() { return new Etudiant("Rakoto", 21); }
```

Une exception levée par la méthode devient une réponse JSON `{"code":500,"erreur":"...","message":"..."}`.

### Upload de fichiers

Le formulaire doit avoir `enctype="multipart/form-data"`.

```java
public String upload(@Param("photo") Fichier photo, @Param("documents") List<Fichier> documents) { ... }
```

Un `Fichier` donne `getNom()`, `getExtension()`, `getContentType()`, `getTaille()` et `getContenu()` (les octets).
`Fichier[]` est aussi accepté. Limites : 10 Mo par fichier et 50 Mo par envoi (erreur 400 au-delà).

### Session

```java
public String login(@Param("nom") String nom, Session session) {
    session.setAttribute("utilisateur", nom);
    return "Connecté";
}
```

Une seule instance de `Session` par utilisateur. Méthodes : `getAttribute`, `setAttribute` (une valeur `null`
supprime), `removeAttribute`, `getAttributeNames`, `getId`, `getCreationTime`, `getLastAccessedTime`,
`getMaxInactiveInterval`, `setMaxInactiveInterval`, `isNew`, `invalidate`.
Les données sont gardées par le framework : une JSP ne les voit pas avec `${sessionScope...}`.

### Authentification et rôles : `@Auth`

```java
@Auth                                      // utilisateur connecté, n'importe quel rôle
@Auth(roles = "dg")                        // connecté ET rôle dg
@Auth(roles = {"admin", "dg"})             // connecté ET (admin OU dg)
```

À poser sur une méthode ou sur une classe entière (le `@Auth` de la méthode l'emporte). L'utilisateur est « connecté »
si la session contient une valeur sous `auth-session-key`, et ses rôles sont lus sous `role-session-key`
(un `String`, un tableau ou une liste). Le développeur vérifie le mot de passe dans son propre code, puis range
l'utilisateur et son rôle dans la session.

| Situation | Route HTML | Route `@RestAPI` |
|---|---|---|
| Non connecté | redirection vers `login-url`, ou 401 | 401 en JSON |
| Connecté, mauvais rôle | 403 | 403 en JSON |

## Codes d'erreur

| Code | Cause |
|---|---|
| 400 | valeur de formulaire invalide, fichier trop gros |
| 401 | visiteur non connecté sur une route `@Auth` |
| 403 | rôle insuffisant |
| 500 | exception dans le contrôleur, ou classe d'objet sans constructeur vide |

## Limites connues

- Seulement `GET` et `POST` (une requête `HEAD` provoque une erreur).
- Une URL inconnue affiche la liste des routes avec un code 200, et les réponses HTML des routes sans `@RestAPI`
  commencent par des lignes de debug (« URL trouvée », « Route », « Méthode appelée »).
- Les classes des contrôleurs doivent être dans des dossiers (`WEB-INF/classes`), pas dans un jar.
- Pas de tests automatisés.
