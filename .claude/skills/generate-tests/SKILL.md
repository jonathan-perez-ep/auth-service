---
description: Genera tests unitarios o de integración para una clase Service o Controller del proyecto auth-service. Sigue las convenciones del proyecto automáticamente.
---

El usuario quiere generar tests para una clase Java. Sigue estos pasos:

## 1. Identifica el archivo objetivo

Si el usuario pasó un argumento (ruta o nombre de clase), úsalo. Si no, pregunta qué clase quiere testear.

Lee el archivo objetivo con la herramienta Read.

## 2. Detecta el tipo de clase

- Si el nombre termina en `Service` → genera **tests unitarios** con Mockito
- Si el nombre termina en `Controller` → genera **tests de integración** con MockMvc
- Si no está claro, pregunta al usuario

## 3. Determina la ubicación del test

El test va en `src/test/java/` espejando el paquete del archivo fuente.

Ejemplo:
- Fuente: `src/main/java/ep/example/auth/features/auth/register/RegisterService.java`
- Test:   `src/test/java/ep/example/auth/features/auth/register/RegisterServiceTest.java`

## 4. Genera el test

### Para Services → Test unitario

Convenciones obligatorias:
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` para cada dependencia inyectada
- `@InjectMocks` para la clase bajo test
- Nombres de métodos en inglés: `methodName_withCondition_expectedBehavior()`
- Cubrir: happy path, cada validación que lance excepción, efectos secundarios importantes

Estructura:
```java
@ExtendWith(MockitoExtension.class)
class {ClassName}Test {

    @Mock
    private Dependency1 dependency1;

    @InjectMocks
    private {ClassName} service;

    @Test
    void methodName_withValidInput_performsExpectedAction() { ... }

    @Test
    void methodName_withInvalidInput_throwsIllegalArgumentException() { ... }
}
```

### Para Controllers → Test de integración

Convenciones obligatorias:
- `@SpringBootTest`
- `@AutoConfigureMockMvc` — usar `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `@BeforeEach` para limpiar datos de test (primero tablas hijas por FK, luego tablas padre)
- NO usar `@Transactional` — MockMvc crea transacciones propias que se commitean antes del rollback
- Nombres de métodos en inglés: `endpoint_withCondition_returnsStatusCode()`
- Cubrir: happy path (201/200), validaciones fallidas (400), conflictos (409)

Estructura:
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class {ClassName}IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository; // ajustar según la clase

    @BeforeEach
    void cleanup() {
        // eliminar en orden correcto según FKs
    }

    @Test
    void endpoint_withValidData_returns201() throws Exception { ... }
}
```

## 5. Consideraciones importantes

- Mensajes de error en los `assertThatThrownBy` deben coincidir EXACTAMENTE con los del Service
- Para tests de integración, el `@BeforeEach` debe eliminar solo los datos creados por el test, no datos de otros tests
- Si el Controller tiene `@BeforeEach` que elimina registros con FKs, borrar primero las tablas hijas
- El perfil `test` apunta a `auth_db_test` — asegurarse de que la BD exista

## 6. Verifica que compila

Después de crear el archivo, ejecuta:
```
.\mvnw.cmd test -Dtest=NombreDelTestCreado
```

Si hay errores de compilación, corrígelos antes de reportar como completo.
