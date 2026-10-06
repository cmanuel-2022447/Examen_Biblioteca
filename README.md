# API REST - Sistema de Gestión de Biblioteca

Es el proyecto del examen. Una API para manejar una biblioteca: usuarios, libros y préstamos de libros.
Lo hice con Spring Boot, MySQL y seguridad con JWT.

---

## Qué hace

- Registrar usuarios (que se registren solos quedan como LECTOR).
- Iniciar sesión y que me devuelva un token.
- Ver los libros, buscarlos por título o categoría.
- Crear, editar y borrar libros (solo el ADMIN).
- Prestar un libro y devolverlo (el stock baja y sube).
- Un lector no puede tener más de 3 préstamos activos.
- Si no devuelve a tiempo lo suspenden (sanción) y no puede pedir más hasta que lo regrese.
- Un lector solo ve sus propios préstamos.

---

## Requisitos

- Java 17 o superior (yo uso JDK 21)
- MySQL 8
- Maven
- IntelliJ (lo usé yo) o cualquier editor

---

## Cómo se corre

1. Crear la base de datos en MySQL:

```sql
CREATE DATABASE biblioteca_in5am;
```

Las credenciales están en `src/main/resources/application.properties`:
usuario `IN5AM`, contraseña `_odmon5Am`, base `biblioteca_in5am`.

2. Levantar la aplicación:

```bash
mvn spring-boot:run
```

O desde IntelliJ directo con el botón de run de la clase `BibliotecaApplication`.

> **Ojo:** si me sale el error *"Port 8080 was already in use"* es porque algo
> más está usando el puerto. Uso este script que lo cierra y arranca:
>
> ```powershell
> .\scripts\iniciar-app.ps1
> ```
>
> También pasa que al empaquetar (`mvn package`) el proyecto solo de ese paso
> revisa y libera el puerto (eso está en el `pom.xml`).

La API queda en: `http://localhost:8080`

---

## Usuarios de prueba

La primera vez que arranco se crean solos (están en `DataInitializer`):

| Correo | Contraseña | Rol |
|---|---|---|
| admin@biblioteca.com | Admin123* | ADMIN |
| bibliotecario@biblioteca.com | Bibliotecario123* | BIBLIOTECARIO |
| lector@biblioteca.com | Lector123* | LECTOR |
| lector2@biblioteca.com | Lector123* | LECTOR |

---

## Los endpoints

Básicamente hay que primero hacer login para que me dé el token, y ese token se
pone en la pestaña Authorization de cada petición (o en el header `Authorization: Bearer ...`).

### Autenticación

| Qué | Ruta | Quién puede |
|---|---|---|
| Registrarse | POST `/api/v1/auth/register` | cualquiera |
| Login | POST `/api/v1/auth/login` | cualquiera |

### Libros

| Qué | Ruta | Quién puede |
|---|---|---|
| Listar (con filtros y página) | GET `/api/v1/libros` | cualquiera logueado |
| Ver uno | GET `/api/v1/libros/{id}` | cualquiera logueado |
| Crear | POST `/api/v1/libros` | solo ADMIN |
| Editar | PUT `/api/v1/libros/{id}` | solo ADMIN |
| Borrar | DELETE `/api/v1/libros/{id}` | solo ADMIN |

### Préstamos

| Qué | Ruta | Quién puede |
|---|---|---|
| Prestar | POST `/api/v1/prestamos` | ADMIN y BIBLIOTECARIO |
| Devolver | PATCH `/api/v1/prestamos/{id}/devolucion` | ADMIN y BIBLIOTECARIO |
| Mis préstamos | GET `/api/v1/prestamos/mis-prestamos` | cualquiera (los suyos) |
| Atrasados | GET `/api/v1/prestamos/atrasados` | ADMIN y BIBLIOTECARIO |

Ejemplo de cuerpo para prestar:

```json
{
  "usuarioId": 4,
  "libroId": 1
}
```

---

## Códigos de error

| Código | Significado |
|---|---|
| 400 | Faltan datos o están mal (validaciones) |
| 401 | No tiene token o el token no sirve |
| 403 | Está logueado pero no tiene permiso para eso |
| 404 | No existe el libro o el usuario |
| 409 | Rompe una regla (sin stock, ya tiene 3 préstamos, ya devolvió, sancionado) |

---

## Las pruebas

Hice pruebas con JUnit y usa una base H2 (falsa) para no ensuciar la real:

```bash
mvn test
```

Son 46 y todas pasan. Incluyen una de concurrencia que mete 2 préstamos al
mismo tiempo para ver que no se pisa el stock.

---

## Postman

Está la colección en `postman/Biblioteca_API.postman_collection.json`.
Se importa desde Postman (Import → Files) y ahí vienen los login y todos los
endpoints con sus bodies, y el token se guarda solo cuando haces login.

---

## Cómo está organizado

```
src/main/java/com/examen/biblioteca
├── config/        arranque y datos iniciales
├── controller/    los endpoints
├── dto/           lo que entra y lo que sale
├── entity/        Usuario, Libro, Prestamo (tablas)
├── exception/     errores y sus respuestas
├── repository/    consultas a la base
├── security/      JWT, filtros y configuración de seguridad
└── service/       la lógica y las reglas del negocio
```

Notas mías:
- No usé Lombok, todo a mano.
- La clave del JWT está en `application.properties`.
- El stock se bloquea cuando se hace el préstamo para que dos personas no
  presten el mismo libro al mismo tiempo.
