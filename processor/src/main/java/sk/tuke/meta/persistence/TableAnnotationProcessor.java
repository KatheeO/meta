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
        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, "TableAnnotationProcessor is running!");

        var tableElements = roundEnvironment.getElementsAnnotatedWith(Table.class);

        // Maybe log the found elements:
        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, "Found elements: " + tableElements.size());

        var entities = analyzeEntities(tableElements);
        for (var entity : entities) {
            generateDAO(entity);
        }
        if (!tableElements.isEmpty()) {
            generatePersistenceManager(entities);
        }

        return true;
    }

    private List<EntityStructure> analyzeEntities(Set<? extends Element> tableElements) {
        List<EntityStructure> entities = new ArrayList<>();
        for (Element element : tableElements) {
            // Ensure we are processing a class or interface
            if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.INTERFACE) {
                continue; // Skip if not a class/interface
            }

            TypeElement typeElement = (TypeElement) element; // Cast to TypeElement
            String entityName = typeElement.getSimpleName().toString();
            // Use getQualifiedName for the full package + class name, then extract package
            String qualifiedName = typeElement.getQualifiedName().toString();
            String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));

            EntityStructure entityStructure = new EntityStructure(entityName, packageName);

            // Iterate through enclosed elements (fields, methods...)
            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                // We are interested only in fields
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    VariableElement fieldElement = (VariableElement) enclosedElement;

                    Id idAnnotation = fieldElement.getAnnotation(Id.class);
                    Column columnAnnotation = fieldElement.getAnnotation(Column.class);

                    if (idAnnotation != null || columnAnnotation != null) {
                        String fieldName = fieldElement.getSimpleName().toString();
                        // Determine column name: use @Column name if present, else field name
                        String columnName = (columnAnnotation != null && !columnAnnotation.name().isEmpty())
                                ? columnAnnotation.name()
                                : fieldName;
                        // Get the field type
                        TypeMirror fieldTypeMirror = fieldElement.asType();
                        String fieldJavaType = fieldTypeMirror.toString();
                        // Extract simple type name if needed (e.g., "String" from "java.lang.String")
                        // This might need refinement based on the template
                        if (fieldJavaType.contains(".")) {
                            fieldJavaType = fieldJavaType.substring(fieldJavaType.lastIndexOf('.') + 1);
                        }
                        // Determine if it's an ID column
                        boolean isId = (idAnnotation != null);

                        entityStructure.addColumn(new ColumnStructure(fieldName, columnName, fieldJavaType, isId));
                    }
                }
            }

            // Basic validation: Check if an ID column was found
            try {
                entityStructure.getIdColumn(); // This will throw if no ID was found
                entities.add(entityStructure);
            } catch (IllegalStateException e) {
                processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR,
                        e.getMessage(), typeElement); // Report error linked to the class
            }
        }
        return entities;
    }

    private void generatePersistenceManager(List<EntityStructure> entities) {
        try {
            var javaFile = processingEnv.getFiler().createSourceFile(
                    "sk.tuke.meta.persistence.GeneratedPersistenceManager");
            try (var writer = javaFile.openWriter()) {
                var template = velocity.getTemplate(
                        TEMPLATE_PATH + "/GeneratedPersistenceManager.java.vm");
                VelocityContext context = new VelocityContext();
                context.put("entities", entities);
                template.merge(context, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
