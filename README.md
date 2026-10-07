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
- Git Bash y `jq` (para correr `test-api1.sh`; Git Bash viene con Git for Windows)

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
> más está usando el puerto. Lo libero desde PowerShell:
>
> ```powershell
> $p = Get-NetTCPConnection -LocalPort 8080 -State Listen
> Stop-Process -Id $p.OwningProcess
> ```

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
| 409 | Rompe una regla (sin stock, ya tiene 3 préstamos, ya devolvió, sancionado, email o ISBN ya registrado con otros datos) |

---

## Las pruebas

Hice pruebas con JUnit y usa una base H2 (falsa) para no ensuciar la real:

```bash
mvn test
```

Son 46 y todas pasan. Incluyen una de concurrencia que mete 2 préstamos al
mismo tiempo para ver que no se pisa el stock.

---

## Prueba con el script del examen

`test-api1.sh` hace las pruebas funcionales y de estrés contra la API. Se corre
desde Git Bash con la aplicación ya levantada en el 8080:

```bash
./test-api1.sh
```

Lo que revisa:

1. Registro del lector → debe devolver token
2. Login del admin → token
3. Crear un libro con rol ADMIN → debe crearlo (con acentos incluidos)
4. Catálogo de libros
5. Login del lector
6. Intentar crear libro con lector → **403 Forbidden**
7. Estrés: 100 peticiones concurrentes al catálogo → **`100 200`**

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
- El `pom.xml` trae Lombok (lo pide el examen), pero el código está escrito a
  mano, sin anotaciones de Lombok.
- El registro y la creación de libro son idempotentes: si repito la misma
  petición con los mismos datos me devuelve el resultado en vez de un error
  (el 409 es solo si cambian los datos o la contraseña).
- `JsonEncodingFallbackFilter` acepta cuerpos JSON con acentos que llegan en
  CP1252 desde Git Bash/curl y los pasa a UTF-8 antes de procesarlos.
- La clave del JWT está en `application.properties`.
- El stock se bloquea cuando se hace el préstamo para que dos personas no
  presten el mismo libro al mismo tiempo.
