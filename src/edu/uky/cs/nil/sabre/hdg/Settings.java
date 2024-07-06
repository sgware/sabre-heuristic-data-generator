package edu.uky.cs.nil.sabre.hdg;

/**
 * Important settings and keywords.
 * 
 * @author Stephen G. Ware
 */
public class Settings {

	/** The full name of this software library */
	public static final String TITLE = "Sabre Heuristic Data Generator";
	
	/** The list of primary authors */
	public static final String AUTHORS = "Stephen G. Ware";
	
	/** The major version number comes before the decimal points */
	public static final int MAJOR_VERSION_NUMBER = 1;
	
	/** The minor version number comes after the decimal point */
	public static final int MINOR_VERSION_NUMBER = 0;
	
	/** The full version number (major + minor) as a string */
	public static final String VERSION_STRING = MAJOR_VERSION_NUMBER + "." + MINOR_VERSION_NUMBER;
	
	/** A header including title, authors, and version number */
	public static final String CREDITS = TITLE + " v" + VERSION_STRING + " by " + AUTHORS;
}