# 🕒 Attendance Tracking System

**Attendance Tracking System** es una aplicación **full-stack** diseñada para el **registro y gestión de asistencias**. Implementa un enfoque seguro y escalable, combinando un frontend moderno con Angular y un backend robusto en Java con Spring Boot. Incluye autenticación con **JWT**, control de acceso mediante **Spring Security** y despliegue **contenedorizado con Docker**.

---

## 🚀 Tecnologías utilizadas

### **Frontend**

* 🅰️ **Angular** – Framework para el desarrollo de interfaces reactivas y modulares.
* 🟦 **TypeScript** – Tipado estático que mejora la mantenibilidad y la calidad del código.
* 💅 **HTML5 & CSS3** – Para la estructura y los estilos base del frontend.

### **Backend**

* ☕ **Java** – Lenguaje principal para la lógica del servidor.
* 🌱 **Spring Boot** – Framework que simplifica la configuración y ejecución del backend.
* 🔐 **Spring Security + JWT** – Autenticación y autorización seguras mediante tokens.
* 🧩 **JPA (Java Persistence API)** – Gestión de entidades y operaciones con la base de datos.
* 🐘 **PostgreSQL** – Sistema de base de datos relacional utilizado para almacenar registros de asistencia.
* 🐳 **Docker** – Contenedorización del entorno para despliegue y portabilidad.

---

## 🧩 Arquitectura del proyecto

El backend sigue una **arquitectura MVC (Modelo - Vista - Controlador)**, junto con la **inyección de dependencias de Spring**, asegurando separación de responsabilidades, escalabilidad y mantenibilidad.

* **Entity (Modelo):** Representa las tablas de la base de datos (asistencia, usuarios, roles, etc.).
* **Repository:** Gestiona las operaciones de acceso a datos con JPA.
* **Service:** Contiene la lógica de negocio y orquesta la comunicación entre controladores y repositorios.
* **Controller:** Define los endpoints REST y maneja las peticiones HTTP.
* **Config:** Gestiona la seguridad, JWT y configuración general del proyecto.

---

## 📁 Estructura del proyecto

```
attendance-app/
├── .idea/                    # Configuración del entorno de desarrollo
├── .mvn/                     # Configuración de Maven
├── src/
│   ├── main/
│   │   ├── java/com/attendance/demo/
│   │   │   ├── config/          # Configuración de seguridad, JWT y beans
│   │   │   ├── controller/      # Controladores REST del sistema
│   │   │   ├── dto/             # Objetos de transferencia de datos
│   │   │   ├── entity/          # Entidades JPA (usuarios, asistencia, roles)
│   │   │   ├── exception/       # Manejo personalizado de excepciones
│   │   │   ├── repository/      # Interfaces de acceso a datos (Spring Data JPA)
│   │   │   ├── service/         # Lógica de negocio principal
│   │   │   ├── AttendanceApplication.java  # Punto de entrada de la aplicación
│   │   │   └── prueba.java      # Archivo auxiliar o de pruebas
│   │   └── resources/           # Configuraciones y properties de Spring
│   └── test/                    # Pruebas unitarias e integración
├── target/                      # Archivos compilados (build)
├── pom.xml                      # Dependencias y configuración de Maven
├── mvnw / mvnw.cmd              # Ejecutores de Maven Wrapper
└── .gitignore                   # Archivos y carpetas ignoradas por Git
```

---

## 📌 Características principales

* ✅ **Registro y control de asistencias** para docentes o empleados.
* 🔐 **Autenticación y autorización seguras** mediante JWT.
* 👥 **Gestión de usuarios y roles** con control de acceso.
* 📊 **Consultas y reportes** de asistencia almacenados en PostgreSQL.
* 🧱 Arquitectura basada en **Spring Boot + JPA + PostgreSQL**.
* 🐳 **Despliegue fácil y reproducible** con Docker.

---

## 🌐 Repositorio

🔗 **GitHub:** *(pendiente de publicación)*

---

## 📆 Estado del proyecto

✅ **Completado (2025)**
El sistema se encuentra finalizado, documentado y preparado para su despliegue en entornos locales o en la nube.

---

> 💡 **Attendance Tracking System** combina la potencia del ecosistema Spring con la flexibilidad de Angular, ofreciendo una solución completa y profesional para la gestión de asistencias.
