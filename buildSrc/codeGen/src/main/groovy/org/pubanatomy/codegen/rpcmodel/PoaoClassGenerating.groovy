package com.customshow.codegen.rpcmodel

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

import java.beans.IntrospectionException
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * User: GaryY
 * Date: 6/29/2016*/
class PoaoClassGenerating{
    public PoaoClassGenerating( Class<?> javaClass ){
        this.javaClass = javaClass;
        imports.add( "import mx.events.PropertyChangeEvent;" );
        imports.add( "import com.flashflexpro.as3lib.data.PropertyDesc;" );
    }

    private Class<?> javaClass;


    public String generateCode() throws IntrospectionException{
        String beforeImport = "/**\n" + " * Generated by com.customshow.codegen\n" + " *\n" + " */\n" +
                "package " + StringUtils.join( getPackage(), "." ) + "{\n" + "\n";
        String afterImports = "\n" + "\n[Bindable]" + "\n[RemoteClass(alias=\"" + javaClass.getCanonicalName() +
                "\")]" + "\npublic class " + getClassName() + " extends " + getBaseClassName() + " {\n" +
                "    public function " + getClassName() + "(){\n" + "    }\n" + "\n" +

                getFields() +

                "}\n" + "}";
        addImport( "import " + getBaseClassName() + ";" )
        return beforeImport + getImports() + afterImports
    }

    private ArrayList<String> imports = new ArrayList<>();

    private String getImports(){
        return StringUtils.join( imports.iterator(), "\n" )
    }

    private String getFields() throws IntrospectionException{
        StringBuilder sb = new StringBuilder();

        HashMap<String, PropertyDescriptor> name2desc = new HashMap<>()
        for( PropertyDescriptor propertyDescriptor :
                Introspector.getBeanInfo( javaClass ).getPropertyDescriptors() ){
            name2desc.put( propertyDescriptor.getName(), propertyDescriptor )
        }

        if( !Modifier.isAbstract( javaClass.getModifiers() ) && name2desc.size() == 1 ){
            throw new RuntimeException( javaClass.getCanonicalName() + " has no properties!!!" )
        }

        int count = 0;
        List<String> fieldsLst = new ArrayList<>();
        for( Field eachField : javaClass.getDeclaredFields() ){
            PropertyDescriptor propertyDescriptor = name2desc.get( eachField.getName() )
            if( propertyDescriptor != null ){
                fieldsLst.add( eachField.getName() );
                sb.append( new PoaoPropertyGenerating( eachField, propertyDescriptor, this ).
                        generateCode() );
                count++;
            }
            else if( eachField.getType().isPrimitive() ){
                throw new RuntimeException(
                        javaClass.getCanonicalName() + "." + eachField.getName() + " IS PRIMITIVE, NOT AN OBJECT!!!" )
            }
        }


        String propertiesStr = "new <PropertyDesc>[ PROP_DESC_" + fieldsLst.join( ", \r\n                PROP_DESC_" ) +
                "\r\n            ]";


        final properties = "    private static var _properties:Vector.<PropertyDesc> = null;\n" +
                "    public static function get properties():Vector.<PropertyDesc>{\n" +
                "        if( _properties == null ){\n" + "            _properties = " + propertiesStr + ";\n" +
                "        }\n" + "        return _properties;\n" + "    }\r\n"


        propertiesStr = "";
        if( getBaseClassName() != "EventDispatcher" ){
            propertiesStr += ".concat( " + getBaseClassName() + ".allProperties )"
        }

        final allProperties = "    private static var _allProperties:Vector.<PropertyDesc> = null;\n" +
                "    public static function get allProperties():Vector.<PropertyDesc>{\n" +
                "        if( _allProperties == null ){\n" + "            _allProperties = properties" + propertiesStr +
                ";\n" + "        }\n" + "        return _allProperties;\n" + "    }\r\n"

        sb.insert( 0, properties )
        sb.insert( 0, allProperties )

        return sb.toString();
    }


    public String getClassName(){
        String[] split = javaClass.getCanonicalName().split( "\\." );
        return split[ split.length - 1 ];
    }

    private String getBaseClassName(){
        String superClassName = javaClass.getSuperclass().getCanonicalName();
        boolean equals = superClassName.equals( "java.lang.Object" );
        if( equals ){
            addImport( "import flash.events.EventDispatcher;" );
        }
        return ( equals ? "EventDispatcher" : superClassName );
    }

    public void addImport( String adding ){
        if( imports.indexOf( adding ) < 0 && adding.indexOf( "." ) > 0 && adding.indexOf( "import Vector." ) != 0 ){
            imports.add( adding );
        }
    }

    public String[] getPackage(){
        String[] split = javaClass.getCanonicalName().split( "\\." );
        return ArrayUtils.remove( split, split.length - 1 );
    }
}