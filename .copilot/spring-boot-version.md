# Spring Boot Version Rule

## IMPORTANT: DO NOT MODIFY SPRING BOOT VERSION

The Spring Boot version in this project is set to **3.4.4** and must not be changed under any circumstances.

### Rules:

1. **NO CHANGES ALLOWED**: Do not modify, update, or suggest changes to the Spring Boot version (3.4.4) in the pom.xml file.

2. **PARENT VERSION**: The parent version must remain fixed at:
   ```xml
   <parent>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-parent</artifactId>
       <version>3.4.4</version>
       <relativePath/> <!-- lookup parent from repository -->
   </parent>
   ```

3. **DEPENDENCY VERSIONS**: Do not suggest or implement changes to Spring Boot dependency versions that would implicitly change the Spring Boot version.

4. **SPRING CLOUD COMPATIBILITY**: Ensure that any Spring Cloud dependencies remain compatible with Spring Boot 3.4.4.

### Rationale:

This version has been specifically tested and validated for this application. Changing the version could:
- Introduce compatibility issues with existing dependencies
- Break functionality that relies on specific Spring Boot behavior
- Cause unexpected runtime errors
- Require extensive regression testing

### What to do instead:
1. If you encounter issues with dependencies, try updating individual dependencies while keeping the Spring Boot version constant.
2. Inform the development team about the issue, and they will evaluate whether a version change is necessary.

This rule applies to all Copilot agents and must be strictly followed in all interactions.