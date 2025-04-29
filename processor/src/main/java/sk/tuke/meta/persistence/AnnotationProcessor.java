package sk.tuke.meta.persistence;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor that generates an SQL file for table creation.
 */
@SupportedAnnotationTypes(
        {"sk.tuke.meta.persistence.annotations.Table",
                "sk.tuke.meta.persistence.annotations.Id",
                "sk.tuke.meta.persistence.annotations.Column"}
)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
//@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Use a StringBuilder to collect all the generated SQL commands.
        StringBuilder sqlBuilder = new StringBuilder();
        System.out.println("processing " + annotations.size() + " annotations");

        // Process each class annotated with @Table.
        for (Element element : roundEnv.getElementsAnnotatedWith(Table.class)) {
            System.out.println("processing " + element.getKind());
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement classElement = (TypeElement) element;
            String tableName = getTableName(classElement);
            sqlBuilder.append("CREATE TABLE IF NOT EXISTS '")
                    .append(tableName)
                    .append("' (\n");

            List<String> columnDefs = new ArrayList<>();
            List<String> foreignKeys = new ArrayList<>();

            // Process each field in the class.
            for (Element enclosed : classElement.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD) {
                    continue;
                }
                VariableElement field = (VariableElement) enclosed;

                // If the field is annotated with @Id, generate a primary key column.
                if (field.getAnnotation(Id.class) != null) {
                    columnDefs.add("'" + field.getSimpleName() + "' INTEGER PRIMARY KEY AUTOINCREMENT");
                }
                // If the field is annotated with @Column, generate a simple column.
                else if (field.getAnnotation(Column.class) != null) {
                    String columnName = getColumnName(field);
                    String sqlType = getSQLType(field);
                    // Optionally, you can check for nullable and unique constraints here.
                    columnDefs.add("'" + columnName + "' " + sqlType);
                }
                // Otherwise, if the field represents an entity reference,
                // generate a foreign key column (named fieldName_id) and a constraint.
                else if (isEntityReference(field)) {
                    String columnName = field.getSimpleName() + "_id";
                    columnDefs.add("'" + columnName + "' INTEGER");
                    // Get the referenced table name.
                    TypeElement refElement = (TypeElement) processingEnv.getTypeUtils().asElement(field.asType());
                    String refTableName = getTableName(refElement);
                    foreignKeys.add("FOREIGN KEY('" + columnName + "') REFERENCES '" + refTableName + "'(id)");
                }
            }

            // Combine column definitions and foreign key constraints.
            sqlBuilder.append(String.join(",\n", columnDefs));
            if (!foreignKeys.isEmpty()) {
                sqlBuilder.append(",\n");
                sqlBuilder.append(String.join(",\n", foreignKeys));
            }
            sqlBuilder.append("\n);\n\n");
        }
        System.out.println(sqlBuilder.toString());

        // Write the generated SQL to a resource file in CLASS_OUTPUT.
        try {
            System.out.println("Inside annotation processor before writing generated sql");
            FileObject fileObject = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "generatedTables.sql");
            try (Writer writer = fileObject.openWriter()) {
                writer.write(sqlBuilder.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Error writing generatedTables.sql: " + e.getMessage());
        }

        return true;
    }

    /**
     * Returns the table name for the given class.
     */
    private String getTableName(TypeElement classElement) {
        Table tableAnnotation = classElement.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        } else {
            return classElement.getSimpleName().toString();
        }
    }

    /**
     * Returns the column name for the given field.
     */
    private String getColumnName(VariableElement field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        } else {
            return field.getSimpleName().toString();
        }
    }

    /**
     * Maps the Java type of the field to an SQL type.
     */
    private String getSQLType(VariableElement field) {
        String type = field.asType().toString();
        if (type.equals("int") || type.equals("java.lang.Integer")
                || type.equals("long") || type.equals("java.lang.Long")) {
            return "INTEGER";
        } else if (type.equals("double") || type.equals("java.lang.Double")
                || type.equals("float") || type.equals("java.lang.Float")) {
            return "REAL";
        } else if (type.equals("java.lang.String")) {
            return "TEXT";
        } else {
            return "TEXT";
        }
    }

    /**
     * Determines if a field is an entity reference.
     * This is true if the field type is not a primitive or String and is annotated with @Table.
     */
    private boolean isEntityReference(VariableElement field) {
        // Skip primitives and Strings.
        String fieldType = field.asType().toString();
        if (fieldType.equals("java.lang.String")) {
            return false;
        }
        Element typeElement = processingEnv.getTypeUtils().asElement(field.asType());
        if (typeElement == null) {
            return false;
        }
        return typeElement.getAnnotation(Table.class) != null;
    }
}
