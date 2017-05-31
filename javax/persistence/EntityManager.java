package javax.persistence;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @since 2017-05-31
 * @version 1.0
 * @author marcos morise
 */
public abstract class EntityManager {
    
//*****************************************************************************
// Auxiliar Commands /*********************************************************
//*****************************************************************************

    /**
     * Return Table Definition based in Class name or tableDefinition anotation
     * @param entityType
     * @return Table Definition
     */
    protected static String getTableDefinition(Class entityType) {
        String tableDefinition = "";
        if (entityType.isAnnotationPresent(Entity.class)) {
            tableDefinition = ((Entity) entityType.getAnnotation(Entity.class)).tableDefinition();
        }
        if (tableDefinition.isEmpty()) {
            tableDefinition = entityType.getSimpleName();
        }
        return tableDefinition;
    } //getTableDefinition

    /**
     * Return Column Definition based in field name or columnDefinition anotation
     * @param field
     * @return Table Column Definition
     */
    protected static String getColumnDefinition(Field field) {
        String columnDefinition = "";
        if (field.isAnnotationPresent(Column.class)) {
            columnDefinition = field.getAnnotation(Column.class).columnDefinition();
        } else if (field.isAnnotationPresent(GeneratedValue.class)) {
            columnDefinition = field.getAnnotation(GeneratedValue.class).columnDefinition();
        } else if (field.isAnnotationPresent(Temporal.class)) {
            columnDefinition = field.getAnnotation(Temporal.class).columnDefinition();
        } else if (field.isAnnotationPresent(Lob.class)) {
            columnDefinition = field.getAnnotation(Lob.class).columnDefinition();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            columnDefinition = field.getAnnotation(ManyToOne.class).columnDefinition();
        } else if (field.isAnnotationPresent(Enumerated.class)) {
            columnDefinition = field.getAnnotation(Enumerated.class).columnDefinition();
        }
        if (columnDefinition.isEmpty()) {
            columnDefinition = field.getName();
        }
        return columnDefinition;
    } //getColumnDefinition

    /**
     * Verify if 2 entities values are equals
     * @param entity1
     * @param entity2
     * @return true equals || false even
     * @throws java.sql.SQLException
     */
    protected static Boolean isEntitiesEquals(Object entity1, Object entity2) throws SQLException {
        Boolean equals = true;
        if (entity1.getClass() == entity2.getClass()) {
            Class entityType = entity1.getClass();
            for (int i = 0; i < entityType.getDeclaredFields().length; i++) {
                try {
                    Field field1 = entity1.getClass().getDeclaredFields()[i];
                    Method getter = getFieldGetter(entity1, field1);
                    if (field1.getClass().isAnnotationPresent(Column.class) || field1.getClass().isAnnotationPresent(GeneratedValue.class)) {
                        String value1 = getter.invoke(entity1).toString().trim();
                        String value2 = getter.invoke(entity2).toString().trim();
                        if (value1.compareToIgnoreCase(value2) != 0) {
                            equals = false;
                        }
                    } else if (field1.getClass().isAnnotationPresent(Temporal.class)) {
                        Date value1 = (Date) getter.invoke(entity1);
                        Date value2 = (Date) getter.invoke(entity2);
                        if (value1.compareTo(value2) != 0) {
                            equals = false;
                        }
                    } else if (field1.getClass().isAnnotationPresent(Enumerated.class)) {
                        Enum value1 = (Enum) getter.invoke(entity1);
                        Enum value2 = (Enum) getter.invoke(entity2);
                        if (value1.ordinal() == value2.ordinal()) {
                            equals = false;
                        }
                    } else if (field1.getClass().isAnnotationPresent(Lob.class)) {
                        byte[] value1 = (byte[]) getter.invoke(entity1);
                        byte[] value2 = (byte[]) getter.invoke(entity2);
                        for (int j = 0; j < value1.length; j++) {
                            if (value1[j] == value2[j]) {
                                equals = false;
                            }
                        }
                    } else if (field1.getClass().isAnnotationPresent(ManyToOne.class)) {
                        Object value1 = getter.invoke(entity1);
                        Object value2 = getter.invoke(entity2);
                        if (isEntitiesEquals(value1, value2)) {
                            equals = false;
                        }
                    } else if (field1.getClass().isAnnotationPresent(OneToMany.class)) {
                        List<Object> value1 = (List<Object>) getter.invoke(entity1);
                        List<Object> value2 = (List<Object>) getter.invoke(entity2);
                        for (int j = 0; j < value1.size(); j++) {
                            if (isEntitiesEquals(value1.get(j), value2.get(j))) {
                                equals = false;
                            }
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new SQLException(ex);
                }
            }
        }
        //System.out.println("igual " + equals);
        return equals;
    } //isEntitiesEquals

    /**
     * get id field from class
     * @param entityType
     * @return id field
     */
    protected static Field getClassId(Class entityType) {
        Field fieldId = null;
        for (Field field : entityType.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class) && (field.getType() == Integer.class || field.getType() == Long.class || field.getType() == String.class)) {
                fieldId = field;
            }
        }
        return fieldId;
    } //getClassId

    /**
     * get id field from entity
     * @param entity
     * @return id field
     */
    protected static Field getEntityId(Object entity) {
        Field fieldId = null;
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class) && (field.getType() == Integer.class || field.getType() == Long.class || field.getType() == String.class)) {
                fieldId = field;
            }
        }
        return fieldId;
    } //getEntityId

    /**
     * get Field method Getter
     * @param entity
     * @param field
     * @return method getter
     */
    protected static Method getFieldGetter(Object entity, Field field) {
        Method getter;
        String fieldName = field.getName().substring(0, 1).toUpperCase() + (field.getName().length() > 1 ? field.getName().substring(1) : "");
        try {
            getter = entity.getClass().getMethod("get" + fieldName);
        } catch (NoSuchMethodException | SecurityException ex) {
            getter = null;
        }
        return getter;
    } //getFieldGetter

    /**
     * get Field method Setter
     *
     * @param entity
     * @param field
     * @return method setter
     */
    protected static Method getFieldSetter(Object entity, Field field) {
        Method setter;
        String fieldName = field.getName().substring(0, 1).toUpperCase() + (field.getName().length() > 1 ? field.getName().substring(1) : "");
        try {
            setter = entity.getClass().getMethod("set" + fieldName, field.getType());
        } catch (NoSuchMethodException | SecurityException ex) {
            setter = null;
        }
        return setter;
    } //getFieldSetter

//*****************************************************************************
// SQL Update Commands ********************************************************
//*****************************************************************************
    
    /**
     * entityToStatement
     *
     * @param entity
     * @param statement
     * @return entityCount
     * @throws SQLException
     */
    protected static int entityToStatement(Object entity, PreparedStatement statement) throws SQLException {
        //Setting values for statement
        int i = 0;
        for (Field field : entity.getClass().getDeclaredFields()) {
            try {
                if (!field.isAnnotationPresent(GeneratedValue.class)
                        && ((field.isAnnotationPresent(Column.class) && (field.getType() == Boolean.class || field.getType() == Byte.class || field.getType() == Short.class || field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Float.class || field.getType() == Double.class || field.getType() == Character.class || field.getType() == String.class))
                        || (field.isAnnotationPresent(Temporal.class) && field.getType() == Date.class)
                        || (field.isAnnotationPresent(Enumerated.class) && field.getType().getSuperclass() == Enum.class)
                        || (field.isAnnotationPresent(Lob.class) && field.getType() == byte[].class)
                        || field.isAnnotationPresent(ManyToOne.class))) {
                    Method getter = getFieldGetter(entity, field);
                    i++;
                    if (field.getType() == Boolean.class) {
                        statement.setBoolean(i, (Boolean) getter.invoke(entity));
                    } else if (field.getType() == Byte.class) {
                        statement.setByte(i, (Byte) getter.invoke(entity));
                    } else if (field.getType() == Short.class) {
                        statement.setShort(i, (Short) getter.invoke(entity));
                    } else if (field.getType() == Integer.class) {
                        statement.setInt(i, (Integer) getter.invoke(entity));
                    } else if (field.getType() == Long.class) {
                        statement.setLong(i, (Long) getter.invoke(entity));
                    } else if (field.getType() == Float.class) {
                        statement.setFloat(i, (Float) getter.invoke(entity));
                    } else if (field.getType() == Double.class) {
                        statement.setDouble(i, (Double) getter.invoke(entity));
                    } else if (field.getType() == Character.class) {
                        statement.setString(i, (String) getter.invoke(entity));
                    } else if (field.getType() == String.class) {
                        statement.setString(i, (String) getter.invoke(entity));
                    } else if (field.getType() == byte[].class) {
                        statement.setBlob(i, new java.io.ByteArrayInputStream((byte[]) getter.invoke(entity)));
                    } else if (field.getType().getSuperclass() == Enum.class) {
                        statement.setInt(i, ((Enum) getter.invoke(entity)).ordinal());
                    } else if (field.getType() == Date.class) {
                        if (field.getAnnotation(Temporal.class).value() == TemporalType.TIME) {
                            statement.setTime(i, new java.sql.Time(((Date) getter.invoke(entity)).getTime()));
                        } else if (field.getAnnotation(Temporal.class).value() == TemporalType.DATE) {
                            statement.setDate(i, new java.sql.Date(((Date) getter.invoke(entity)).getTime()));
                        } else if (field.getAnnotation(Temporal.class).value() == TemporalType.TIMESTAMP) {
                            statement.setTimestamp(i, java.sql.Timestamp.from(((Date) getter.invoke(entity)).toInstant()));
                        }
                    } else if (field.isAnnotationPresent(ManyToOne.class)) {
                        Object agregateObj = getter.invoke(entity);
                        Field agregateObjId = getEntityId(agregateObj);
                        Method getterId = getFieldGetter(agregateObj, agregateObjId);
                        if (agregateObjId.getType() == Integer.class) {
                            statement.setInt(i, (Integer) getterId.invoke(agregateObj));
                        } else if (agregateObjId.getType() == Long.class) {
                            statement.setLong(i, (Long) getterId.invoke(agregateObj));
                        } else if (agregateObjId.getType() == String.class) {
                            statement.setString(i, (String) getterId.invoke(agregateObj));
                        }
                    }
                }
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                //ex.printStackTrace();
                throw new SQLException(ex.getMessage());
            }
        }
        return i;
    } //entityToStatement

    /**
     * Create Table SQL Command
     *
     * @param owner
     * @param ownerField
     * @param child
     * @return
     */
    protected static String createTableSQL(Class owner, Field ownerField, Class child) {
        String sql = "";
        String primaryKey = "";
        String foreignKeys = "";
        String tableDefinition = getTableDefinition(child);
        sql += "CREATE TABLE " + tableDefinition + " (\n";

        //if is child, add OneToMany owner column
        if (owner != null && ownerField != null) {
            //set name column
            String fieldName;
            if (ownerField.getAnnotation(OneToMany.class).joinColumn().isEmpty()) {
                fieldName = owner.getSimpleName() + "_" + getClassId(owner).getName();
            } else {
                fieldName = ownerField.getAnnotation(OneToMany.class).joinColumn();
            }
            //set type column
            Field classOwnerId = getClassId(owner);
            if (classOwnerId.getType() == Integer.class) {
                sql += "    " + fieldName + " INT NOT NULL,\n";
            } else if (classOwnerId.getType() == Long.class) {
                sql += "    " + fieldName + " BIGINT NOT NULL,\n";
            } else {
                if (classOwnerId.getAnnotation(Column.class).length() > 0) {
                    sql += "    " + fieldName + " VARCHAR(" + classOwnerId.getAnnotation(Column.class).length() + ") NOT NULL,\n";
                } else {
                    sql += "    " + fieldName + " LONG VARCHAR NOT NULL,\n";
                }
            }
            foreignKeys += "    CONSTRAINT FKA_" + tableDefinition + "_" + getTableDefinition(owner) + " FOREIGN KEY (" + fieldName + ") REFERENCES " + getTableDefinition(owner) + " ON DELETE CASCADE,\n";
        }//add OneToMany owner column

        //add other columns
        for (Field field : child.getDeclaredFields()) {
            String fieldName = getColumnDefinition(field);
            if (field.isAnnotationPresent(Id.class)) {
                primaryKey = "    CONSTRAINT PK_" + tableDefinition + " PRIMARY KEY (" + fieldName + "),\n";
            }
            //Generated Value
            if (field.isAnnotationPresent(GeneratedValue.class)) {
                if (field.getType() == Integer.class) {
                    sql += "    " + fieldName + " INT NOT NULL GENERATED ALWAYS AS IDENTITY(START WITH " + field.getAnnotation(GeneratedValue.class).startWith() + ", INCREMENT BY " + field.getAnnotation(GeneratedValue.class).incrementBy() + "),\n";
                } else if (field.getType() == Long.class) {
                    sql += "    " + fieldName + " BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY(START WITH " + field.getAnnotation(GeneratedValue.class).startWith() + ", INCREMENT BY " + field.getAnnotation(GeneratedValue.class).incrementBy() + "),\n";
                }
            } else if (field.isAnnotationPresent(Enumerated.class)) {
                //Enumerated Type
                if (field.getType().getSuperclass() == Enum.class) {
                    sql += "    " + fieldName + " INT NOT NULL,\n";
                }
            } else if (field.isAnnotationPresent(Temporal.class)) {
                //Temporal Type
                if (field.getType() == Date.class) {
                    if (field.getAnnotation(Temporal.class).value() == TemporalType.DATE) {
                        sql += "    " + fieldName + " DATE NOT NULL,\n";
                    } else if (field.getAnnotation(Temporal.class).value() == TemporalType.TIME) {
                        sql += "    " + fieldName + " TIME NOT NULL,\n";
                    } else if (field.getAnnotation(Temporal.class).value() == TemporalType.TIMESTAMP) {
                        sql += "    " + fieldName + " TIMESTAMP NOT NULL,\n";
                    }
                }
            } else if (field.isAnnotationPresent(Lob.class)) {
                //Lob Type
                if (field.getType() == byte[].class) {
                    sql += "    " + fieldName + " BLOB NOT NULL,\n";
                }
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                //ManyToOne Type
                Class joinClass = field.getType();
                Field joinClassId = null;
                for (Field joinClassField : joinClass.getDeclaredFields()) {
                    if (joinClassField.isAnnotationPresent(Id.class)) {
                        joinClassId = joinClassField;
                    }
                }
                if (joinClassId != null) {
                    if (joinClassId.getType() == String.class) {
                        if (field.getAnnotation(Column.class).length() > 0) {
                            sql += "    " + fieldName + " VARCHAR(" + field.getAnnotation(Column.class).length() + ") NOT NULL,\n";
                        } else {
                            sql += "    " + fieldName + " LONG VARCHAR NOT NULL,\n";
                        }
                    } else if (joinClassId.getType() == Integer.class) {
                        sql += "    " + fieldName + " INT NOT NULL,\n";
                    } else {
                        sql += "    " + fieldName + " BIGINT NOT NULL,\n";
                    }
                }
                foreignKeys += "    CONSTRAINT FKA_" + tableDefinition + "_" + fieldName + " FOREIGN KEY (" + fieldName + ") REFERENCES " + getTableDefinition(field.getType()) + " ON DELETE CASCADE,\n";
            } else if (field.isAnnotationPresent(Column.class)) {
                //Column Type
                if (field.getType() == Boolean.class) {
                    sql += "    " + fieldName + " BOOLEAN" + (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "") + ",\n";
                } else if ((field.getType() == Byte.class || field.getType() == Short.class) && field.isAnnotationPresent(Column.class)) {
                    sql += "    " + fieldName + " SMALLINT" + (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "") + ",\n";
                } else if (field.getType() == Integer.class) {
                    sql += "    " + fieldName + " INT" + (field.getAnnotation(Column.class).unique() ? " UNIQUE NOT NULL" : (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "")) + ",\n";
                } else if (field.getType() == Long.class) {
                    sql += "    " + fieldName + " BIGINT" + (field.getAnnotation(Column.class).unique() ? " UNIQUE NOT NULL" : (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "")) + ",\n";
                } else if (field.getType() == Float.class) {
                    sql += "    " + fieldName + " FLOAT" + (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "") + ",\n";
                } else if (field.getType() == Double.class) {
                    sql += "    " + fieldName + " DOUBLE" + (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "") + ",\n";
                } else if (field.getType() == Character.class) {
                    sql += "    " + fieldName + " CHAR" + (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "") + ",\n";
                } else if (field.getType() == String.class) {
                    if (field.getAnnotation(Column.class).length() > 0) {
                        sql += "    " + fieldName + " VARCHAR(" + field.getAnnotation(Column.class).length() + ")" + (field.getAnnotation(Column.class).unique() ? " UNIQUE NOT NULL" : (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "")) + ",\n";
                    } else {
                        sql += "    " + fieldName + " LONG VARCHAR" + (field.getAnnotation(Column.class).unique() ? " UNIQUE NOT NULL" : (!field.getAnnotation(Column.class).nullable() ? " NOT NULL" : "")) + ",\n";
                    }
                }
            }
        } //add other columns
        //endding sql
        sql += primaryKey + foreignKeys;
        sql = sql.substring(0, sql.length() - 2) + "\n";
        sql += ")";

        System.out.println("\n" + sql + "\n");

        return sql;
    }//createTableSQL

        /**
     * CreateTable used to create Tables child from entity OneToMany with SQL cmd pre-defined
     * Used to Modify API to support other Databases
     * @param connection
     * @param owner
     * @param ownerField
     * @param child
     * @param sql
     * @throws SQLException 
     */
    protected static void createTable(Connection connection, Class owner, Field ownerField, Class child, String sql) throws SQLException {
        if (child.isAnnotationPresent(Entity.class)) {
            //String sql = createTableSQL(owner, ownerField, child);
            //Execute Create Table SQL command
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();

            //Create OneToMany table childs
            for (Field field : child.getDeclaredFields()) {
                if (field.isAnnotationPresent(OneToMany.class)) {
                    createTable(connection, child, field, (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                }
            }
        }
    }//create table
    /**
     * CreateTable used to create Tables child from entity OneToMany
     * @param connection
     * @param owner
     * @param ownerField
     * @param child
     * @throws SQLException
     */
    protected static void createTable(Connection connection, Class owner, Field ownerField, Class child) throws SQLException {
        if (child.isAnnotationPresent(Entity.class)) {
            String sql = createTableSQL(owner, ownerField, child);
            createTable(connection, owner, ownerField, child, sql);
        }
    }
    
    /**
     * Create database from entityType
     * @param connection
     * @param entityType
     * @throws SQLException
     */
    public static void createTable(Connection connection, Class entityType) throws SQLException {
        createTable(connection, null, null, entityType);
    } //create table

    /**
     * insert SQL command
     * @param owner
     * @param ownerField
     * @param entity
     * @return 
     */
    protected static String[] insertSQL(Object owner, Field ownerField, Object entity) {
        String sql = "";
        String selectIdentity = "";
        String values = "";
        String tableDefinition = getTableDefinition(entity.getClass());

        //Create sql command
        sql += "INSERT INTO " + tableDefinition + " (";
        //Insert parameters
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(GeneratedValue.class)
                    && ((field.isAnnotationPresent(Column.class) && (field.getType() == Boolean.class || field.getType() == Byte.class || field.getType() == Short.class || field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Float.class || field.getType() == Double.class || field.getType() == Character.class || field.getType() == String.class))
                    || (field.isAnnotationPresent(Temporal.class) && field.getType() == Date.class)
                    || (field.isAnnotationPresent(Enumerated.class) && field.getType().getSuperclass() == Enum.class)
                    || (field.isAnnotationPresent(Lob.class) && field.getType() == byte[].class)
                    || field.isAnnotationPresent(ManyToOne.class))) {
                sql += getColumnDefinition(field) + ",";
                values += "?,";
            }
            if (field.isAnnotationPresent(GeneratedValue.class)) {
                selectIdentity = "SELECT IDENTITY_VAL_LOCAL() FROM " + tableDefinition;
            }
        }
        //If is OneToMany child entity
        if (owner != null && ownerField != null) {
            if (!ownerField.getAnnotation(OneToMany.class).joinColumn().isEmpty()) {
                sql += ownerField.getAnnotation(OneToMany.class).joinColumn() + ",";
            } else {
                sql += owner.getClass().getSimpleName() + "_" + (getEntityId(owner).getName()) + ",";
            }
            values += "?,";
        }
        //Finalize sql command creation
        sql = sql.substring(0, sql.length() - 1);
        values = values.substring(0, values.length() - 1);
        sql += ")\nVALUES (" + values + ")";

        //Prepare to Insert Entity Into Database
        System.out.println("\n" + sql + "\n" + selectIdentity + "\n");
        return new String []{sql, selectIdentity};
    }//insertSQL

    /**
     * Private Insert used to insert child from entity OneToMany with SQL cmd pre-defined
     * Used to Modify API to support other Databases
     * @param connection
     * @param owner
     * @param ownerField
     * @param entity
     * @param sqlCmd
     * @throws SQLException 
     */
    protected static void insert(Connection connection, Object owner, Field ownerField, Object entity, String sqlCmd[]) throws SQLException {
        if (entity.getClass().isAnnotationPresent(Entity.class)) {
            try {
                //String sqlCmd[] = insertSQL(owner, ownerField, entity);
                String sql = sqlCmd[0];
                String selectIdentity = sqlCmd[1];
                PreparedStatement statement = connection.prepareStatement(sql);
                int i = entityToStatement(entity, statement);
                //If is OneToMany child entity
                if (owner != null && ownerField != null) {
                    i++;
                    Field ownerId = getEntityId(owner);
                    Method getterOwnerId = getFieldGetter(owner, ownerId);
                    //Define owner parameter
                    if (ownerId.getType() == Integer.class) {
                        statement.setInt(i, (Integer) getterOwnerId.invoke(owner));
                    } else if (ownerId.getType() == Long.class) {
                        statement.setLong(i, (Long) getterOwnerId.invoke(owner));
                    } else if (ownerId.getType() == String.class) {
                        statement.setString(i, (String) getterOwnerId.invoke(owner));
                    }
                }

                //Insert Entity Into Database
                statement.executeUpdate();
                statement.close();

                //set Generated Value to Field
                if (!selectIdentity.isEmpty()) {
                    statement = connection.prepareStatement(selectIdentity);
                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        Field fId = getEntityId(entity);
                        Method setterId = getFieldSetter(entity, fId);
                        if (fId.getType() == Integer.class) {
                            setterId.invoke(entity, rs.getInt(1));
                        } else if (fId.getType() == Long.class) {
                            setterId.invoke(entity, rs.getLong(1));
                        }
                    }
                    statement.close();
                }

                //Insert OneToMany childs
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        List listChild = (List) getFieldGetter(entity, field).invoke(entity);
                        for (Object entityChild : listChild) {
                            insert(connection, entity, field, entityChild);
                        }
                    }
                }

            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
                //ex.printStackTrace();
                throw new SQLException(ex);
            }

        }
    } //insert
    
    /**
     * Private Insert used to insert child from entity OneToMany
     * @param connection
     * @param owner
     * @param ownerField
     * @param entity
     * @throws SQLException
     */
    protected static void insert(Connection connection, Object owner, Field ownerField, Object entity) throws SQLException {
        String sqlCmd[] = insertSQL(owner, ownerField, entity);
        insert(connection, owner, ownerField, entity, sqlCmd);
    }
    
    /**
     * Insert Entity Into Database
     * @param connection
     * @param entity
     * @throws SQLException
     */
    public static void insert(Connection connection, Object entity) throws SQLException {
        insert(connection, null, null, entity);
    } //insert

    /**
     * Update Entity from Database
     * @param connection
     * @param entity
     * @throws SQLException
     */
    public static void update(Connection connection, Object entity) throws SQLException {
        if (entity.getClass().isAnnotationPresent(Entity.class)) {
            try {
                String sql = "";
                String tableDefinition = getTableDefinition(entity.getClass());
                Field entityId = getEntityId(entity);
                Method getterId = getFieldGetter(entity, entityId);
                Object entityVerified = null;

                //Verify OneToMany childs from db entity to insert or update
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        if (entityVerified == null) {
                            entityVerified = load(connection, entity.getClass(), entityId.getName() + " = " + getterId.invoke(entity));
                        }
                        List listChild = (List) getFieldGetter(entity, field).invoke(entity);
                        List listChildVerified = (List) getFieldGetter(entityVerified, field).invoke(entityVerified);
                        for (Object entityChild : listChild) {
                            Method getterChildId = getFieldGetter(entityChild, getEntityId(entityChild));
                            Object valueId = getterChildId.invoke(entityChild);
                            if (valueId == null) {
                                insert(connection, entity, field, entityChild);
                            } else {
                                for (Object entityChildVerified : listChildVerified) {
                                    Method getterChildVerifiedId = getFieldGetter(entityChildVerified, getEntityId(entityChildVerified));
                                    Object valueVerifiedId = getterChildVerifiedId.invoke(entityChildVerified);
                                    if (valueId.equals(valueVerifiedId) && !isEntitiesEquals(entityChild, entityChildVerified)) {
                                        update(connection, entityChild);
                                    }
                                }
                            }
                        }
                    }
                }
                //Verify removed OneToMany childs
                if (entityVerified != null) {
                    for (Field field : entityVerified.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(OneToMany.class)) {
                            List listChild = (List) getFieldGetter(entity, field).invoke(entity);
                            List listChildVerified = (List) getFieldGetter(entityVerified, field).invoke(entityVerified);
                            for (Object entityChildVerified : listChildVerified) {
                                Boolean exists = false;
                                for (Object entityChild : listChild) {
                                    Method getterChildId = getFieldGetter(entityChild, getEntityId(entityChild));
                                    Method getterChildVerifiedId = getFieldGetter(entityChildVerified, getEntityId(entityChildVerified));
                                    Object valueId = getterChildId.invoke(entityChild);
                                    Object valueVerifiedId = getterChildVerifiedId.invoke(entityChildVerified);
                                    if (valueId != null && valueVerifiedId != null && valueId.equals(valueVerifiedId)) {
                                        exists = true;
                                    }
                                }
                                if (!exists) {
                                    delete(connection, entityChildVerified);
                                    //listChildVerified.remove(entityChildVerified);
                                }
                            }
                        }
                    }
                }
                //Update command
                //Create sql command
                sql += "UPDATE " + tableDefinition + " SET ";
                //Insert parameters
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (!field.isAnnotationPresent(GeneratedValue.class)
                            && ((field.isAnnotationPresent(Column.class) && (field.getType() == Boolean.class || field.getType() == Byte.class || field.getType() == Short.class || field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Float.class || field.getType() == Double.class || field.getType() == Character.class || field.getType() == String.class))
                            || (field.isAnnotationPresent(Temporal.class) && field.getType() == Date.class)
                            || (field.isAnnotationPresent(Enumerated.class) && field.getType().getSuperclass() == Enum.class)
                            || (field.isAnnotationPresent(Lob.class) && field.getType() == byte[].class)
                            || field.isAnnotationPresent(ManyToOne.class))) {
                        sql += "\n" + getColumnDefinition(field) + " = ?,";
                    }
                }

                //Finalize sql command creation
                sql = sql.substring(0, sql.length() - 1);
                sql += "\nWHERE " + getColumnDefinition(entityId) + "= ?";

                //Prepare to Update Entity from Database
                System.out.println("\n" + sql + "\n");
                PreparedStatement statement = connection.prepareStatement(sql);
                int i = entityToStatement(entity, statement);
                i++;
                if (entityId.getType() == Integer.class) {
                    statement.setInt(i, (Integer) getterId.invoke(entity));
                } else if (entityId.getType() == Long.class) {
                    statement.setLong(i, (Long) getterId.invoke(entity));
                } else if (entityId.getType() == String.class) {
                    statement.setString(i, (String) getterId.invoke(entity));
                }

                //Update Entity from Database
                statement.executeUpdate();
                statement.close();

            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
                //ex.printStackTrace();
                throw new SQLException(ex);
            }

        }
    } //update

    /**
     * Delete Entity From Database
     * @param connection
     * @param entity
     * @throws SQLException
     */
    public static void delete(Connection connection, Object entity) throws SQLException {
        if (entity.getClass().isAnnotationPresent(Entity.class)) {
            try {
                String sql = "";
                String tableDefinition = getTableDefinition(entity.getClass());
                Field entityId = getEntityId(entity);
                Method getter = getFieldGetter(entity, entityId);

                //Create sql command
                sql += "DELETE FROM " + tableDefinition + " WHERE " + getColumnDefinition(entityId) + " = ?";

                //Prepare to Delete Entity Into Database
                System.out.println("\n" + sql + "\n");
                PreparedStatement statement = connection.prepareStatement(sql);

                if (entityId.getType() == Integer.class) {
                    statement.setInt(1, (Integer) getter.invoke(entity));

                } else if (entityId.getType() == Long.class) {
                    statement.setLong(1, (Long) getter.invoke(entity));

                } else if (entityId.getType() == String.class) {
                    statement.setString(1, (String) getter.invoke(entity));
                }

                //Delete Entity Database
                statement.executeUpdate();
                statement.close();
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
                //ex.printStackTrace();
                throw new SQLException(ex);
            }

        }
    } //delete

//*****************************************************************************
// SQL Query Commands ********************************************************
//*****************************************************************************
    
    /**
     * SQL Resultset to Entity
     * @param connection
     * @param resultSet
     * @param entity
     * @throws SQLException 
     */
    protected static void resultSetToEntity(Connection connection, ResultSet resultSet, Object entity) throws SQLException {
        try {
            Class entityType = entity.getClass();
            // Column | Id | Enumerated | Temporal | Lob Type
            for (Field field : entityType.getDeclaredFields()) {
                Method setter = getFieldSetter(entity, field);

                if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(GeneratedValue.class) || field.isAnnotationPresent(Enumerated.class) || field.isAnnotationPresent(Temporal.class) || field.isAnnotationPresent(Lob.class)) {
                    if (field.getType() == Boolean.class) {
                        setter.invoke(entity, resultSet.getBoolean(getColumnDefinition(field)));
                    } else if (field.getType() == Byte.class || field.getType() == Short.class) {
                        setter.invoke(entity, resultSet.getShort(getColumnDefinition(field)));
                    } else if (field.getType() == Integer.class) {
                        setter.invoke(entity, resultSet.getInt(getColumnDefinition(field)));
                    } else if (field.getType() == Long.class) {
                        setter.invoke(entity, resultSet.getLong(getColumnDefinition(field)));
                    } else if (field.getType() == Float.class) {
                        setter.invoke(entity, resultSet.getFloat(getColumnDefinition(field)));
                    } else if (field.getType() == Double.class) {
                        setter.invoke(entity, resultSet.getDouble(getColumnDefinition(field)));
                    } else if (field.getType() == Character.class) {
                        setter.invoke(entity, resultSet.getString(getColumnDefinition(field)));
                    } else if (field.getType() == String.class) {
                        setter.invoke(entity, resultSet.getString(getColumnDefinition(field)));
                    } else if (field.getType().getSuperclass() == Enum.class) {
                        setter.invoke(entity, field.getType().getEnumConstants()[resultSet.getInt(getColumnDefinition(field))]);
                    } else if (field.getType() == Date.class) {
                        setter.invoke(entity, resultSet.getDate(getColumnDefinition(field)));
                    } else if (field.getType() == byte[].class) {
                        setter.invoke(entity, resultSet.getBytes(getColumnDefinition(field)));
                    }
                }
            } // Column | Id | Temporal | Enumerated | Lob Type 

            //ManyToOne | OneToMany
            for (Field field : entityType.getDeclaredFields()) {
                Method setter = getFieldSetter(entity, field);

                if (field.isAnnotationPresent(ManyToOne.class)) {
                    //ManyToOne
                    Field fieldId = getClassId(field.getType());
                    if (fieldId.getType() == Integer.class) {
                        setter.invoke(entity, load(connection, field.getType(), fieldId.getName() + "=" + resultSet.getInt(getColumnDefinition(field))));
                    } else if (fieldId.getType() == Long.class) {
                        setter.invoke(entity, load(connection, field.getType(), fieldId.getName() + "=" + resultSet.getLong(getColumnDefinition(field))));
                    } else if (fieldId.getType() == String.class) {
                        setter.invoke(entity, load(connection, field.getType(), fieldId.getName() + "='" + resultSet.getString(getColumnDefinition(field)) + "'"));
                    }
                } else if (field.isAnnotationPresent(OneToMany.class)) {
                    //OneToMany
                    String fieldName;
                    if (!field.getAnnotation(OneToMany.class).joinColumn().isEmpty()) {
                        fieldName = field.getAnnotation(OneToMany.class).joinColumn();
                    } else {
                        fieldName = entity.getClass().getSimpleName() + "_" + (getEntityId(entity).getName());
                    }
                    Field entityId = getEntityId(entity);
                    Class fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (entityId.getType() == Integer.class) {
                        setter.invoke(entity, loadList(connection, fieldType, fieldName + "=" + getFieldGetter(entity, getEntityId(entity)).invoke((entity))));
                    } else if (entityId.getType() == Long.class) {
                        setter.invoke(entity, loadList(connection, fieldType, fieldName + "=" + getFieldGetter(entity, getEntityId(entity)).invoke((entity))));
                    } else if (entityId.getType() == String.class) {
                        setter.invoke(entity, loadList(connection, fieldType, fieldName + "='" + getFieldGetter(entity, getEntityId(entity)).invoke((entity)) + "'"));
                    }
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            //ex.printStackTrace();
            throw new SQLException(ex);
        }
    } //resultSetToEntity

    /**
     * Load object
     * @param connection
     * @param entityType
     * @param condition
     * @return object loaded or null
     * @throws SQLException
     */
    public static <T> T load(Connection connection, Class<T> entityType, String condition) throws SQLException {
        Object entity = null;

        if (entityType.isAnnotationPresent(Entity.class) && !condition.isEmpty()) {

            String sql = "";
            String tableDefinition = getTableDefinition(entityType);

            //Create sql command
            sql += "SELECT ";

            for (Field field : entityType.getDeclaredFields()) {
                if ((field.isAnnotationPresent(Column.class) && (field.getType() == Boolean.class || field.getType() == Byte.class || field.getType() == Short.class || field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Float.class || field.getType() == Double.class || field.getType() == Character.class || field.getType() == String.class))
                        || (field.isAnnotationPresent(GeneratedValue.class) && (field.getType() == Integer.class || field.getType() == Long.class))
                        || (field.isAnnotationPresent(Temporal.class) && field.getType() == Date.class)
                        || (field.isAnnotationPresent(Enumerated.class) && field.getType().getSuperclass() == Enum.class)
                        || (field.isAnnotationPresent(Lob.class) && field.getType() == byte[].class)
                        || field.isAnnotationPresent(ManyToOne.class)) {
                    sql += getColumnDefinition(field) + ",";
                }
            }
            sql = sql.substring(0, sql.length() - 1) + "\n";
            sql += "FROM " + tableDefinition + "\n";
            sql += "WHERE " + condition;

            //execute SQL
            System.out.println("\n" + sql + "\n");
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            //Populate Entity
            if (resultSet.next()) {
                try {
                    entity = entityType.getConstructor().newInstance();
                    resultSetToEntity(connection, resultSet, entity);
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    //ex.printStackTrace();
                    throw new SQLException(ex);
                }
            }

        }
        return entityType.cast(entity);
    } //load

    /**
     * Load Object List
     * @param connection
     * @param entityType
     * @param condition
     * @return Object List
     * @throws SQLException
     */
    public static <T> List<T> loadList(Connection connection, Class<T> entityType, String condition) throws SQLException {
        //Class fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        List<T> entityList = new java.util.ArrayList<>();

        if (entityType.isAnnotationPresent(Entity.class)) {

            String sql = "";
            String tableDefinition = getTableDefinition(entityType);

            //Create sql command
            sql += "SELECT ";

            for (Field field : entityType.getDeclaredFields()) {
                if ((field.isAnnotationPresent(Column.class) && (field.getType() == Boolean.class || field.getType() == Byte.class || field.getType() == Short.class || field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Float.class || field.getType() == Double.class || field.getType() == Character.class || field.getType() == String.class))
                        || (field.isAnnotationPresent(GeneratedValue.class) && (field.getType() == Integer.class || field.getType() == Long.class))
                        || (field.isAnnotationPresent(Temporal.class) && field.getType() == Date.class)
                        || (field.isAnnotationPresent(Enumerated.class) && field.getType().getSuperclass() == Enum.class)
                        || (field.isAnnotationPresent(Lob.class) && field.getType() == byte[].class)
                        || field.isAnnotationPresent(ManyToOne.class)) {
                    sql += getColumnDefinition(field) + ",";
                }
            }
            sql = sql.substring(0, sql.length() - 1) + "\n";
            sql += "FROM " + tableDefinition + "\n";
            sql += condition.isEmpty() ? "" : "WHERE " + condition;

            //execute SQL
            System.out.println("\n" + sql + "\n");
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            //Populate Entity
            while (resultSet.next()) {
                try {
                    Object entity = entityType.getConstructor().newInstance();
                    resultSetToEntity(connection, resultSet, entity);
                    entityList.add(entityType.cast(entity));
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    //ex.printStackTrace();
                    throw new SQLException(ex);
                }
            }
        }
        return entityList;
    } //loadList

    /**
     * Select from Database with native types result
     * @param connection
     * @param query
     * @return
     * @throws SQLException
     */
    protected static List select(Connection connection, String query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery(query);
        List result = new java.util.ArrayList();
        while (rs.next()) {
            List line = new java.util.ArrayList();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                switch (rs.getMetaData().getColumnType(i)) {
                    case java.sql.Types.BOOLEAN:
                    case java.sql.Types.BIT:
                        line.add(rs.getBoolean(i));
                        break;
                    case java.sql.Types.TINYINT:
                    case java.sql.Types.SMALLINT:
                        line.add(rs.getShort(i));
                        break;
                    case java.sql.Types.INTEGER:
                        line.add(rs.getInt(i));
                        break;
                    case java.sql.Types.BIGINT:
                        line.add(rs.getLong(i));
                        break;
                    case java.sql.Types.NUMERIC:
                    case java.sql.Types.DECIMAL:
                    case java.sql.Types.FLOAT:
                    case java.sql.Types.REAL:
                        line.add(rs.getFloat(i));
                        break;
                    case java.sql.Types.DOUBLE:
                        line.add(rs.getDouble(i));
                        break;
                    case java.sql.Types.DATE:
                        line.add(new java.util.Date(rs.getDate(i).getTime()));
                        break;
                    case java.sql.Types.TIME:
                        line.add(new java.util.Date(rs.getTime(i).getTime()));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        line.add(new java.util.Date(rs.getTimestamp(i).getTime()));
                        break;
                    case java.sql.Types.BLOB:
                    case java.sql.Types.CLOB:
                        line.add(rs.getBytes(i));
                        break;
                    default:
                        line.add(rs.getString(i));
                        break;
                }
            }
            if (line.size() == 1) {
                result.add(line.get(0));
            } else {
                result.add(line);
            }
        }
        statement.close();
        return result;
    }

    /**
     * Select from Database with formatted result
     * @param connection
     * @param query
     * @return
     * @throws SQLException
     */
    protected static List selectF(Connection connection, String query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery(query);
        List result = new java.util.ArrayList();
        while (rs.next()) {
            List line = new java.util.ArrayList();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                switch (rs.getMetaData().getColumnType(i)) {
                    case java.sql.Types.BOOLEAN:
                    case java.sql.Types.BIT:
                        line.add(rs.getBoolean(i));
                        break;
                    case java.sql.Types.TINYINT:
                    case java.sql.Types.SMALLINT:
                        line.add(rs.getShort(i));
                        break;
                    case java.sql.Types.INTEGER:
                        line.add(rs.getInt(i));
                        break;
                    case java.sql.Types.BIGINT:
                        line.add(rs.getLong(i));
                        break;
                    case java.sql.Types.NUMERIC:
                    case java.sql.Types.DECIMAL:
                    case java.sql.Types.REAL:
                    case java.sql.Types.FLOAT:
                        line.add(new java.text.DecimalFormat("#,##0.00").format(rs.getFloat(i)));
                        break;
                    case java.sql.Types.DOUBLE:
                        line.add(new java.text.DecimalFormat("#,##0.#########").format(rs.getDouble(i)));
                        break;
                    case java.sql.Types.DATE:
                        line.add(new java.text.SimpleDateFormat("dd/MM/yyyy").format(rs.getDate(i)));
                        break;
                    case java.sql.Types.TIME:
                        line.add(new java.text.SimpleDateFormat("HH:mm:ss").format(rs.getTime(i)));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        line.add(new java.util.Date(rs.getTimestamp(i).getTime()));
                        break;
                    case java.sql.Types.BLOB:
                    case java.sql.Types.CLOB:
                        try {
                            java.awt.image.BufferedImage bf = javax.imageio.ImageIO.read(rs.getBlob(i).getBinaryStream());
                            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                            javax.imageio.ImageIO.write(bf, "PNG", output);
                            line.add(java.util.Base64.getEncoder().encodeToString(output.toByteArray()));
                        } catch (java.io.IOException | NullPointerException | java.sql.SQLException e) {
                            line.add("");
                        }
                        break;
                    default:
                        line.add(rs.getString(i));
                        break;
                }
            }
            if (line.size() == 1) {
                result.add(line.get(0));
            } else {
                result.add(line);
            }
        }
        statement.close();
        return result;
    }

//*****************************************************************************
// JSON Commands **************************************************************
//*****************************************************************************

    /**
     * Select from Database and convert toJSON
     * @param connection
     * @param query
     * @return
     */
    public static String SelectToJSON(Connection connection, String query) {
        String json = "";
        try {
            json += "[\n";
            List result = selectF(connection, query);
            for (int i = 0; i < result.size(); i++) {
                List line = (List) result.get(i);
                json += "    [";
                for (int j = 0; j < line.size(); j++) {
                    Object o = line.get(j);
                    if (o.getClass() == String.class) {
                        json += "\"" + line.get(j) + "\",";
                    } else {
                        json += line.get(j) + ",";
                    }
                    json = json.substring(0, json.length() - 1);
                }
                json += "]\n";
            }
            json += "]\n";
        } catch (SQLException ex) {
            json = "";
        }
        return json;
    }

    /**
     * Entity to JSON
     * @param entity
     * @return json
     */
    public static String toJSON(Object entity) {
        String json = "";
        try {
            json += "{\n";

            //Insert fields
            for (Field field : entity.getClass().getDeclaredFields()) {
                if ((field.isAnnotationPresent(Column.class) && (field.getType() == Boolean.class || field.getType() == Byte.class || field.getType() == Short.class || field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Float.class || field.getType() == Double.class || field.getType() == Character.class || field.getType() == String.class))
                        || (field.isAnnotationPresent(Temporal.class) && field.getType() == Date.class)
                        || (field.isAnnotationPresent(Enumerated.class) && field.getType().getSuperclass() == Enum.class)
                        || (field.isAnnotationPresent(Lob.class) && field.getType() == byte[].class)
                        || field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(GeneratedValue.class)) {
                    json += "\"" + field.getName() + "\" : ";
                    if (field.getType() == Boolean.class
                            || field.getType() == Byte.class
                            || field.getType() == Short.class
                            || field.getType() == Integer.class
                            || field.getType() == Long.class
                            || field.getType() == Float.class
                            || field.getType() == Double.class) {
                        json += getFieldGetter(entity, field).invoke(entity);
                    } else if (field.getType() == Character.class
                            || field.getType() == String.class) {
                        json += "\"" + getFieldGetter(entity, field).invoke(entity) + "\"";
                    } else if (field.getType() == byte[].class) { //toBase64
                        byte[] blobField = (byte[]) getFieldGetter(entity, field).invoke(entity);
                        if (blobField != null && blobField.length > 0) {
                            java.awt.image.BufferedImage bf = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(blobField));
                            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                            javax.imageio.ImageIO.write(bf, "PNG", output);
                            String base64 = java.util.Base64.getEncoder().encodeToString(output.toByteArray());
                            json += "\"data:image/png;base64," + base64 + "\"";
                        } else {
                            json += "\"\"";
                        }
                    } else if (field.getType().getSuperclass() == Enum.class) {
                        json += ((Enum) getFieldGetter(entity, field).invoke(entity)).ordinal();
                    } else if (field.getType() == Date.class) {
                        if (field.getAnnotation(Temporal.class).value() == TemporalType.TIME) {
                            json += "\"" + new SimpleDateFormat("hh:mm:ss").format(getFieldGetter(entity, field).invoke(entity)) + "\"";
                        } else if (field.getAnnotation(Temporal.class).value() == TemporalType.DATE) {
                            json += "\"" + new SimpleDateFormat("yyyy-MM-dd").format(getFieldGetter(entity, field).invoke(entity)) + "\"";
                        } else if (field.getAnnotation(Temporal.class).value() == TemporalType.TIMESTAMP) {
                            json += ((Date) getFieldGetter(entity, field).invoke(entity)).toInstant();
                        }
                    } else if (field.isAnnotationPresent(ManyToOne.class)) {
                        json += toJSON(getFieldGetter(entity, field).invoke(entity));
                    } else if (field.isAnnotationPresent(OneToMany.class)) {
                        json += "[ \n";
                        List<Object> entityList = (List<Object>) getFieldGetter(entity, field).invoke(entity);
                        for (Object o : entityList) {
                            json += toJSON(o) + ",\n";
                        }
                        json = json.length() > 2 ? json.substring(0, json.length() - 2) : json;
                        json += "\n]";
                    }
                    json += ",\n";
                }
            }
            json = json.length() > 2 ? json.substring(0, json.length() - 2) + "\n}" : "";

        } catch (IOException | IllegalArgumentException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            //ex.printStackTrace();
            json = "";
        }
        return json;
    }

    /**
     * JSON to Entity
     * @param entityType
     * @param json
     * @return 
     */
    public static Object fromJSON(Class entityType, String json) {
        Object o = null;
        if (entityType.isAnnotationPresent(Entity.class) && json.length() > 4) {
            try {
                //Instance object
                o = entityType.getConstructor().newInstance();
                System.out.println(o.getClass().getName());
                //prepare json
                json = json.replaceAll("\n", "");
                //exists { } then remove, else isn't json
                if (json.startsWith("{") && json.endsWith("}")) {
                    json = json.substring(1);
                    json = json.substring(0, json.length() - 1);
                } else {
                    o = null;
                }
                //if is valid json
                if (o != null) {
                    //Normalize json splited
                    String[] jsonSplits = json.split(",");
                    List<String> jsons = new java.util.ArrayList();
                    for (int i = 0; i < jsonSplits.length; i++) {
                        if (jsonSplits[i].contains("[")) {
                            String jsonPiece = jsonSplits[i] + ",";
                            do {
                                i++;
                                jsonPiece += jsonSplits[i] + ",";
                            } while (!jsonSplits[i].contains("]"));
                            jsonPiece = jsonPiece.substring(0, jsonPiece.length() - 1);
                            jsons.add(jsonPiece);
                        } else if (jsonSplits[i].contains("{")) {
                            String jsonPiece = jsonSplits[i] + ",";
                            do {
                                i++;
                                jsonPiece += jsonSplits[i] + ",";
                            } while (!jsonSplits[i].contains("}"));
                            jsonPiece = jsonPiece.substring(0, jsonPiece.length() - 1);
                            jsons.add(jsonPiece);
                        } else if (jsonSplits[i].contains("base64")) {
                            jsons.add(jsonSplits[i] + "," + jsonSplits[++i]);
                        } else {
                            jsons.add(jsonSplits[i]);
                        }
                    }

                    //Separate json in tokens
                    for (String jsonTokens : jsons) {
                        //Separate field and value
                        String[] fieldSplit = jsonTokens.split(":");
                        String fieldName = fieldSplit[0].replaceAll("\"", "").trim();
                        if (fieldName.startsWith("{")) {
                            fieldName = fieldName.substring(1);
                        }
                        String jsonValue = "";
                        for (int i = 1; i < fieldSplit.length; i++) {
                            jsonValue += fieldSplit[i].trim() + ":";
                        }
                        //jsonValue = jsonValue.length()>0?jsonValue.substring(0, jsonValue.length() - 1):jsonValue;
                        jsonValue = jsonValue.substring(0, jsonValue.length() - 1);
                        if (jsonValue.startsWith("\"") && jsonValue.endsWith("\"")) {
                            jsonValue = jsonValue.substring(1);
                            jsonValue = jsonValue.substring(0, jsonValue.length() - 1);
                        }
                        jsonValue = jsonValue.trim();

                        //Get Field and Method
                        Field field = o.getClass().getDeclaredField(fieldName);
                        Object value = null;
                        Method method = getFieldSetter(o, field);

                        //Get value by type var
                        if (method != null && jsonValue.compareTo("null") != 0) {
                            if (field.getType() == Boolean.class) {
                                value = Boolean.parseBoolean(jsonValue);
                            } else if (field.getType() == Byte.class) {
                                value = Byte.parseByte(jsonValue);
                            } else if (field.getType() == Short.class) {
                                value = Short.parseShort(jsonValue);
                            } else if (field.getType() == Integer.class) {
                                value = Integer.parseInt(jsonValue);
                            } else if (field.getType() == Long.class) {
                                value = Long.parseLong(jsonValue);
                            } else if (field.getType() == Float.class) {
                                value = Float.parseFloat(jsonValue);
                            } else if (field.getType() == Double.class) {
                                value = Double.parseDouble(jsonValue);
                            } else if (field.getType() == Character.class) {
                                value = jsonValue.length() > 0 ? jsonValue.charAt(0) : ' ';
                            } else if (field.getType() == String.class) {
                                value = jsonValue;
                            } else if (field.isAnnotationPresent(Enumerated.class)) {
                                value = field.getType().getEnumConstants()[Integer.parseInt(jsonValue)];
                            } else if (field.isAnnotationPresent(Temporal.class) && field.getAnnotation(Temporal.class).value() == TemporalType.TIME) {
                                value = new SimpleDateFormat("hh:mm:ss").parse(jsonValue);
                            } else if (field.isAnnotationPresent(Temporal.class) && field.getAnnotation(Temporal.class).value() == TemporalType.DATE) {
                                value = new SimpleDateFormat("yyyy-MM-dd").parse(jsonValue);
                            } else if (field.isAnnotationPresent(Temporal.class) && field.getAnnotation(Temporal.class).value() == TemporalType.TIMESTAMP) {
                                value = new Date(Long.parseLong(jsonValue));
                            } else if (field.getType() == byte[].class) { //toBase64
                                if (!jsonValue.isEmpty()) {
                                    jsonValue = jsonValue.replaceAll("data:image/png;base64,", "");
                                    value = java.util.Base64.getDecoder().decode(jsonValue);
                                }
                            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                                if (jsonValue.startsWith("{") && jsonValue.endsWith("}")) {
                                    value = fromJSON(field.getType(), jsonValue);
                                } else {
                                    value = null;
                                }
                            } else if (field.isAnnotationPresent(OneToMany.class)) {
                                if (jsonValue.startsWith("[") && jsonValue.endsWith("]")) {
                                    jsonValue = jsonValue.substring(1);
                                    jsonValue = jsonValue.substring(0, jsonValue.length() - 1);
                                    jsonValue = jsonValue.trim();
                                    List<Object> entityList = new java.util.ArrayList<>();
                                    for (String splitValue : jsonValue.split("}")) {
                                        splitValue = splitValue.trim();
                                        if (splitValue.startsWith(",")) {
                                            splitValue = splitValue.substring(1);
                                        }
                                        Class elementType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                                        Object element = fromJSON(elementType, splitValue + "}");
                                        entityList.add(element);
                                    }
                                    value = entityList;
                                } else {
                                    value = null;
                                }
                            }
                            //Invoke method if value != null
                            if (value != null) {
                                method.invoke(o, value);
                            }
                        }
                    }
                }
            } catch (ParseException | NumberFormatException | NoSuchFieldException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                //ex.printStackTrace();
                o = null;
            }
        }
        return o;
    }

} //EntityManager
