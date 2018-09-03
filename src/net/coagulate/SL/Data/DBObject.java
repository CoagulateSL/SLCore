package net.coagulate.SL.Data;

import java.util.Map;
import java.util.TreeMap;
import net.coagulate.SL.DBException;
import net.coagulate.SL.Log;
import net.coagulate.SL.SystemException;

/** Abstract superclass for all the database things that represent tables, if there's common code.
 * Specifically, things that represent rows, identified by a unique numeric ID.
 * Non instansible tables tend to just use static methods and extend Database rather than DBObject.
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class DBObject extends Database implements Comparable {

    protected int id;
    
    protected DBObject() {}// used to access subclass data before we 'exist'
    
    public int getId() { return id; }

    protected abstract String getTableName();
    protected abstract String getIdField();
    protected String getString(String columnname) { return getString(true,columnname); }
    protected String getString(boolean mandatory,String columnname) { 
        return dqs(mandatory,"select "+columnname+" from "+getTableName()+" where "+getIdField()+"=?",getId());
    }
    protected boolean getBool(String columnname) { return getBool(true,columnname); }
    protected boolean getBool(boolean mandatory,String columnname) {
        Integer val=getInt(mandatory,columnname);
        if (val==null || val==0) { return false; }
        if (val==1) { return true; }
        throw new SystemException("Unexpected value "+val+" parsing DB boolean field selfmodify on attribute "+this);

    }
    protected void set(String columnname,Boolean value) {
        set(columnname,(value?1:0));
    }
    protected Integer getInt(String columnname) { return getInt(true,columnname); }
    protected Integer getInt(boolean mandatory,String columnname) {
        return dqi(mandatory,"select "+columnname+" from "+getTableName()+" where "+getIdField()+"=?",getId());
    }
    protected void set(String columnname,String value) {
        d("update "+getTableName()+" set "+columnname+"=? where "+getIdField()+"=?",value,getId());
    }
    protected void set(String columnname,Integer value) {
        d("update "+getTableName()+" set "+columnname+"=? where "+getIdField()+"=?",value,getId());
    }
    // master query (dc - database count) - unpacks the resultset into a Results object containing Rows, and then closes everything out
    /** Count number of results, assumes "select count(*) from TableName where " prefix to the command
     * @param whereclause SQL following "where " keyword
     * @param params SQL parameters
     * @return integer number of rows matched
     */
    public int dc(String whereclause,Object... params)
    {
        return dqi(true,"select count(*) from "+getTableName()+" where "+whereclause,params);
    }    
    
    boolean validated=false;
    /** Verify this DB Object has backing in the database.
     * Specifically checks the ID matches one and only one row, as it should.
     * Only checks once, after which it shorts and returns ASAP. (sets a flag).
     */
    public void validate() {
        if (validated) { return; }
        int count=dqi(false,"select count(*) from "+getTableName()+" where "+getIdField()+"=?",getId());
        if (count>1) { throw new TooMuchDataException("Too many rows - got "+count+" instead of 1 while validating "+getTableName()+" - "+getId()); }
        if (count<1) { throw new NoDataException("No rows - got "+count+" instead of 1 while validating "+getTableName()+" - "+getId()); }
        validated=true;
    }
    // we use a factory style design to make sure that the same object is always returned
    // that is, if you load "entity 1", which is say "iain maltz" in region "somewhere" or whatever
    // if you look up the user "iain maltz" in "somewhere", you'll get back a copy of entity 1
    // these two entities are always the exact same, as in literally "entity==entity" as well as "entity.equals(entity)"
    
    // this is where we store our factory data, indexed by Type (string), ID (int) and then the DBObject subclass
    protected static Map<String,Map<Integer,DBObject>> factory=new TreeMap<String,Map<Integer,DBObject>>();
    
    /** Thread safe putter into the factory.  Also the getter.
     * NOTE - this method returns an object.  this may not be the same as the object you are putting, if someone beat you to it.
     * this is how we avoid race conditions creating two copies of the same, only one call can be in factoryPut() at once, whichever
     * one gets there first will be stored, and the 2nd caller will simply get the first "put"s object back, which it should 
     * use in preference to its own created one.
     * @param type Class name for object type
     * @param id ID number
     * @param store DBObject or subclass to store
     * @return DBObject that should be used, not necessarily the one that was stored
     */
    protected static synchronized DBObject factoryPut(String type,int id,DBObject store) {
        if (id==0) { throw new SystemException("ID zero is expressly prohibited, does not exist, and suggests a programming bug."); }
        if (!factory.containsKey(type)) {
            // we need this to avoid null pointer below
            Map<Integer,DBObject> innermap=new TreeMap<>();
            factory.put(type,innermap);
        }
        // does this ID exist already
        if (!factory.get(type).containsKey(id)) {
            // no - store it
            factory.get(type).put(id,store);
        }
        // return, either the previous value, or the one we just put there
        return factory.get(type).get(id);
    }
    public abstract String getNameField();
    public String getName() {
        return getString(getNameField());
    }
    public String getNameSafe() {
        try {
            return getName();
        } catch (DBException ex) {
            Log.error("DATABASE/getNameSafe()","SAFE MODE SQLEXCEPTION",ex);
            return "SQLEXCEPTION";
        }
    }
    
    @Override
    /** Provide a sorting order based on names.
     * Implements the comparison operator for sorting (TreeSet etc)
     * We rely on the names as the sorting order, and pass the buck to String.compareTo()
     */
    public int compareTo(Object t) {
        if (!DBObject.class.isAssignableFrom(t.getClass())) {
            throw new SystemException(t.getClass().getName()+" is not assignable from DBObject");
        }
        String ours=getNameSafe();
        DBObject them=(DBObject)t;
        String theirs=them.getNameSafe();
        return ours.compareTo(theirs);
    }
    
}
