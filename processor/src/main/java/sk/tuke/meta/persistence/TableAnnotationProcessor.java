package sk.tuke.meta.persistence;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic; // Ensure this is imported
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes("sk.tuke.meta.persistence.annotations.Table")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TableAnnotationProcessor extends AbstractProcessor {
    private static final String TEMPLATE_PATH = "sk/tuke/meta/persistence/templates";

    private VelocityEngine velocity;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        velocity = new VelocityEngine();
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "TableAnnotationProcessor is running!");

        Set<? extends Element> tableElements = roundEnvironment.getElementsAnnotatedWith(Table.class);

        if (tableElements.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "No elements annotated with @Table found in this round.");
            return false; // No tables to process in this round
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found @Table elements: " + tableElements.size());

        // 1. Collect all known entity fully qualified names from this round
        // This set will store the fully qualified names of all classes annotated with @Table
        Set<String> knownEntityQualifiedNames = new HashSet<>();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Table.class)) { // Iterate again to be sure
            if (element instanceof TypeElement) {
                knownEntityQualifiedNames.add(((TypeElement) element).getQualifiedName().toString());
            }
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Known entity types in this round: " + knownEntityQualifiedNames);


        // 2. Analyze entities, passing the set of known entity types
        List<EntityStructure> entities = analyzeEntities(tableElements, knownEntityQualifiedNames);

        if (entities.isEmpty() && !tableElements.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "No valid entity structures were created from @Table elements.");
        }


        for (EntityStructure entity : entities) {
            generateDAO(entity);
        }

        // Generate PersistenceManager only if there are entities to manage.
        // It's better to check the 'entities' list which contains successfully analyzed structures.
        if (!entities.isEmpty()) {
            generatePersistenceManager(entities);
        }

        return true; // Indicate that the annotations have been processed
    }

    // Modified to accept knownEntityQualifiedNames
    private List<EntityStructure> analyzeEntities(Set<? extends Element> tableElements, Set<String> knownEntityQualifiedNames) {
        List<EntityStructure> entities = new ArrayList<>();
        for (Element element : tableElements) {
            // Ensure we are processing a class
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Element " + element.getSimpleName() + " annotated with @Table is not a class. Skipping.", element);
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            String entityName = typeElement.getSimpleName().toString();
            String qualifiedName = typeElement.getQualifiedName().toString();
            String packageName = "";

            if (qualifiedName.contains(".")) {
                packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
            } else {
                // Class is in the default package
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Entity " + entityName + " is in the default package. This is generally discouraged.", typeElement);
            }

            EntityStructure entityStructure = new EntityStructure(entityName, packageName);

            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    VariableElement fieldElement = (VariableElement) enclosedElement;

                    Id idAnnotation = fieldElement.getAnnotation(Id.class);
                    Column columnAnnotation = fieldElement.getAnnotation(Column.class);

                    if (idAnnotation != null || columnAnnotation != null) {
                        String fieldName = fieldElement.getSimpleName().toString();
                        String columnNameInAnnotation = (columnAnnotation != null && !columnAnnotation.name().isEmpty())
                                ? columnAnnotation.name()
                                : fieldName; // Default to field name if @Column name is empty

                        TypeMirror fieldTypeMirror = fieldElement.asType();
                        String fullFieldTypeName = fieldTypeMirror.toString(); // Fully qualified type name (e.g., sk.tuke.meta.example.Department)

                        // Get simple type name for $column.JavaType (used for casting in template, e.g., "Department")
                        String simpleFieldJavaType = fullFieldTypeName;
                        if (simpleFieldJavaType.contains(".")) {
                            simpleFieldJavaType = simpleFieldJavaType.substring(simpleFieldJavaType.lastIndexOf('.') + 1);
                        }

                        boolean isId = (idAnnotation != null);

                        // *** Determine if it's an entity reference ***
                        // Check if the fully qualified type name of the field is in our set of known entity types
                        boolean isEntityRef = knownEntityQualifiedNames.contains(fullFieldTypeName);

                        // ----- ADD THIS DETAILED LOGGING -----
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                String.format("Entity '%s', Field '%s': Type FQDN is '%s'. Checking against known entities: %s. Is it a ref? %s",
                                        entityName,
                                        fieldName,
                                        fullFieldTypeName,
                                        knownEntityQualifiedNames.toString(),
                                        isEntityRef),
                                element); // 'element' here refers to the class TypeElement for context

                        // ----- END OF ADDED LOGGING -----

                        if (isEntityRef) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                    "Field '" + fieldName + "' in entity '" + entityName +
                                            "' of type '" + simpleFieldJavaType + "' (FQ: " + fullFieldTypeName + ") IS an entity reference.", element);
                        } else if (columnAnnotation != null) { // Only log for @Column fields that are not entity refs for clarity
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                    "Field '" + fieldName + "' in entity '" + entityName +
                                            "' of type '" + simpleFieldJavaType + "' (FQ: " + fullFieldTypeName + ") is NOT an entity reference.", element);
                        }


                        // Pass isEntityRef to ColumnStructure constructor
                        entityStructure.addColumn(new ColumnStructure(fieldName, columnNameInAnnotation, simpleFieldJavaType, isId, isEntityRef));
                    }
                }
            }

            try {
                entityStructure.getIdColumn(); // This will throw if no ID was found
                entities.add(entityStructure);
            } catch (IllegalStateException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Entity " + entityName + ": " + e.getMessage(), typeElement);
            }
        }
        return entities;
    }

    private void generatePersistenceManager(List<EntityStructure> entities) {
        if (entities.isEmpty()) { // Defensive check
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "No entities to generate PersistenceManager for.");
            return;
        }
        try {
            var javaFile = processingEnv.getFiler().createSourceFile(
                    "sk.tuke.meta.persistence.GeneratedPersistenceManager");
            try (var writer = javaFile.openWriter()) {
                var template = velocity.getTemplate(
                        TEMPLATE_PATH + "/GeneratedPersistenceManager.java.vm");
                VelocityContext context = new VelocityContext();
                context.put("entities", entities);
                template.merge(context, writer);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Successfully generated GeneratedPersistenceManager.java");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate PersistenceManager: " + e.getMessage());
            // Consider not re-throwing RuntimeException to allow other processing to complete
            // throw new RuntimeException(e);
        }
    }

    private void generateDAO(EntityStructure entity) {
        try {
            var javaFile = processingEnv.getFiler().createSourceFile(
                    entity.getFullDaoName());
            try (var writer = javaFile.openWriter()) {
                var template = velocity.getTemplate(TEMPLATE_PATH + "/DAO.java.vm");
                VelocityContext context = new VelocityContext();
                context.put("entity", entity);
                template.merge(context, writer);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Successfully generated DAO: " + entity.getFullDaoName());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate DAO for " + entity.getName() + ": " + e.getMessage());
            // throw new RuntimeException(e);
        }
    }
}