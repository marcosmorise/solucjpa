package javax.persistence.mysql;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;

/**
 * Entity Manager MySQL
 * @since 2017-05-31
 * @version 1.0
 * @author marcos morise
 */
public abstract class EntityManager extends javax.persistence.EntityManager {

//*****************************************************************************
// SQL Update Commands ********************************************************
//*****************************************************************************
    /**
     * CreateTable used to create Tables child from entity OneToMany
     *
     * @param connection
     * @param owner
     * @param ownerField
     * @param child
     * @throws SQLException
     */
    protected static void createTable(Connection connection, Class owner, Field ownerField, Class child) throws SQLException {
        if (child.isAnnotationPresent(Entity.class)) {
            String sql = createTableSQL(owner, ownerField, child);
            
            //***************************************
            //***************************************
            for (Field field : child.getDeclaredFields()) {
                if (field.isAnnotationPresent(GeneratedValue.class)) {
                    if (field.getType() == Long.class) {
                        sql = sql.replaceAll(
                                " BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY(START WITH " + field.getAnnotation(GeneratedValue.class).startWith() + ", INCREMENT BY " + field.getAnnotation(GeneratedValue.class).incrementBy() + ")", 
                                " BIGINT NOT NULL AUTO_INCREMENT"
                        );
                    } else {
                        sql = sql.replaceAll(
                                " INT NOT NULL GENERATED ALWAYS AS IDENTITY(START WITH " + field.getAnnotation(GeneratedValue.class).startWith() + ", INCREMENT BY " + field.getAnnotation(GeneratedValue.class).incrementBy() + ")", 
                                " INT NOT NULL AUTO_INCREMENT"
                        );
                    }
                }
            }
            //***************************************
            createTable(connection, owner, ownerField, child, sql);
        }
    }

    /**
     * Private Insert used to insert child from entity OneToMany
     *
     * @param connection
     * @param owner
     * @param ownerField
     * @param entity
     * @throws SQLException
     */
    protected static void insert(Connection connection, Object owner, Field ownerField, Object entity) throws SQLException {
        String tableDefinition = getTableDefinition(entity.getClass());
        String sqlCmd[] = insertSQL(owner, ownerField, entity);
        //***************************************
        sqlCmd[1] = sqlCmd[1].replaceAll(
                "SELECT IDENTITY_VAL_LOCAL() FROM " + tableDefinition, 
                "SELECT LAST_INSERT_ID()"
        );
        //***************************************
        insert(connection, owner, ownerField, entity, sqlCmd);
    }

} //EntityManager
