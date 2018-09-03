package net.coagulate.SL.Data;

/** This feels like a bodge :).
 * If we wish to push a "null" into the database, then we pass a "null" reference to our methods.
 * Unfortunately the database methods infer the "column type" from the object's class, and null has no class.
 * raw null is assumed to be a null string by the DB api layer, so we need an actual reference to represent the null integer.
 * any instance of an object that is "instanceof NullInteger" is assumed to be  a null value of an integer type by the DB api.
 * Naturally integer/null support is a little patchy, and primitive types do not support 'null integers'.
 *
 * On the other hand, google searching even just for "NullInteger.java" has a few matches, so other people must have found some need.
 * 
 * @author Iain Price <gphud@predestined.net>
 */
public class NullInteger {

}
