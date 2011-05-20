package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula;

public interface PAULAXMLStructure {

	//tags and attributes for files of type TEXT(text.dtd)
	public static final String TAG_TEXT_BODY= "body";		//Tagname des Tags body
	public static final String TAG_TEXT_BODY_OPEN = "<"+TAG_TEXT_BODY+">";
	public static final String TAG_TEXT_BODY_CLOSE = "</"+TAG_TEXT_BODY+">";
	
	//specification of all paula tags and attributes
	public static final String ATT_ID=	"id";				//Attribut id
	
	//generic tags and attributes for all PAULA documents (Header)
	public static final String TAG_HEADER= "header";				//Tagname des Tags header
	public static final String[] ATT_HEADER_PAULA_ID= {"paula_id", "sfb_id"};	//Attributname des Attributes header.paula_id
	public static final String ATT_HEADER_ID=	"id";				//Attributname des Attributes header.id
	public static final String ATT_HEADER_TYPE=	"type";				//Attributname des Attributes header.id
	public static final String TAG_HEADER_XML = "<?xml version=\"1.0\" standalone=\"no\"?>";
	public static final String TAG_PAULA_OPEN = "<paula version=\"1.0\">" ;
	
	//tags and attributes for files of PAULA type MARK(mark.dtd)
	public static final String TAG_MARK_MARKLIST= 	"marklist";			//Tagname des Tags markList
	public static final String ATT_MARK_MARKLIST_BASE= 	"xml:base";	//Attributname des Attributs markList.base	
	public static final String ATT_MARK_MARKLIST_TYPE= 	"type";		//Attributname des Attributs markList.type
	public final static String MARK_LIST_CLOSE_TAG = "</"+TAG_MARK_MARKLIST+">";
	
	public static final String TAG_MARK_MARK= 			"mark";		//Tagname des Tags mark
	public static final String ATT_MARK_MARK_ID= 		"id";			//Attributname des Attributs mark.id
	public static final String ATT_MARK_MARK_HREF= 	"xlink:href";		//Attributname des Attributs mark.href
	public static final String ATT_MARK_MARK_TYPE= 	"type";		//Attributname des Attributs mark.type
	public static final String VALUE_MARK_MARK_TYPE= 	"virtual";		//Attributname des Attributs mark.type
	
	
	//tags from dtd paula_rel.dtd
	public static final String TAG_REL_RELLIST= 	"rellist";			//Tagname des Tags relList
	public static final String ATT_REL_RELLIST_BASE= 	"xml:base";	//Attributname des Attributs markList.base	
	public static final String ATT_REL_RELLIST_TYPE= 	"type";		//Attributname des Attributs markList.type
	
	public static final String TAG_REL_REL= 		"rel";			//Tagname des Tags rel
	public static final String ATT_REL_REL_ID= 		"id";				//Attributname des Attributs rel.id
	public static final String ATT_REL_REL_HREF= 	"xlink:href";		//Attributname des Attributs rel.href
	public static final String ATT_REL_REL_TARGET= 	"target";		//Attributname des Attributs rel.href
	
	//tags and attributes for files of PAULA type STRUCT(struct.dtd)
	public static final String TAG_STRUCT_STRUCTLIST= 			"structlist";		//Tagname des Tags structList
	public static final String ATT_STRUCT_STRUCTLIST_BASE= 	"xml:base";		//Attributname des Attributs structList.base	
	public static final String ATT_STRUCT_STRUCTLIST_TYPE= 	"type";			//Attributname des Attributs structList.type
	
	public static final String TAG_STRUCT_STRUCT= 			"struct";		//Tagname des Tags struct
	public static final String ATT_STRUCT_STRUCT_ID= 		"id";			//Attributname des Attributs struct.id

	public static final String TAG_STRUCT_REL= 		"rel";			//Tagname des Tags rel
	public static final String ATT_STRUCT_REL_ID= 		"id";				//Attributname des Attributs rel.id
	public static final String ATT_STRUCT_REL_HREF= 	"xlink:href";		//Attributname des Attributs rel.href
	public static final String ATT_STRUCT_REL_TYPE= 	"type";			//Attributname des Attributs rel.type
	
	//tags and attributes for files of PAULA type FEAT(feat.dtd)
	public static final String TAG_FEAT_FEATLIST= 		"featList";		//Tagname des Tags featList
	public static final String ATT_FEAT_FEATLIST_BASE= "xml:base";		//Attributname des Attributs featList.base	
	public static final String ATT_FEAT_FEATLIST_TYPE= "type";			//Attributname des Attributs featList.type
	
	//tags and attributes for files of PAULA type FEAT(feat.dtd)
	public static final String TAG_FEAT_FEAT= 		"feat";			//Tagname des Tags feat
	public static final String ATT_FEAT_FEAT_ID= 	"id";				//Attributname des Attributs feat.id
	public static final String ATT_FEAT_FEAT_HREF= "xlink:href";		//Attributname des Attributs feat.href
	public static final String ATT_FEAT_FEAT_TAR= 	"target";			//Attributname des Attributs feat.target
	public static final String ATT_FEAT_FEAT_VAL= 	"value";			//Attributname des Attributs feat.value
	public static final String ATT_FEAT_FEAT_DESC= "description";	//Attributname des Attributs feat.description
	public static final String ATT_FEAT_FEAT_EXP= 	"example";		//Attributname des Attributs feat.example
	
	//tags and attributes for files of PAULA type MULTIFEAT(multi.dtd)
	public static final String TAG_MULTI_MULTIFEATLIST= 		"multifeatlist";		//Tagname des Tags featList
	public static final String ATT_MULTI_MULTIFEATLIST_BASE= 	"xml:base";			//Attributname des Attributs featList.base	
	public static final String ATT_MULTI_MULTIFEATLIST_TYPE= 	"type";				//Attributname des Attributs featList.type
	
	//tags and attributes for files of PAULA type MULTIFEAT(feat.dtd)
	public static final String TAG_MULTI_MULTIFEAT= 		"multifeat";		//Tagname des Tags feat
	public static final String ATT_MULTI_MULTIFEAT_ID= 	"id";				//Attributname des Attributs multifeat.id
	public static final String[] ATT_MULTI_MULTIFEAT_HREF= 	{"xlink:href", "href"};		//Attributname des Attributs multifeat.href
	
	//tags and attributes for files of PAULA type MULTIFEAT(feat.dtd)
	public static final String TAG_MULTI_FEAT= 			"feat";		//Tagname des Tags feat
	public static final String ATT_MULTI_FEAT_ID= 			"id";					//Attributname des Attributs feat.id
	public static final String ATT_MULTI_FEAT_NAME= 		"name";		//Attributname des Attributs feat.name
	public static final String ATT_MULTI_FEAT_VALUE= 		"value";		//Attributname des Attributs feat.value
	
	
	
	
	public final static String PAULA_CLOSE_TAG = "</paula>";
	public final static String PAULA_MARK_DOCTYPE_TAG = "<!DOCTYPE paula SYSTEM \"paula_mark.dtd\">";
	public final static String PAULA_TEXT_DOCTYPE_TAG = "<!DOCTYPE paula SYSTEM \"paula_text.dtd\">";
	
	public final static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public final static String PAULA_TOKEN_FILE_CLOSING = new StringBuffer().append("\t")
	  								.append(MARK_LIST_CLOSE_TAG).append(LINE_SEPARATOR)
	  								.append(PAULA_CLOSE_TAG).append(LINE_SEPARATOR).toString();
	
}