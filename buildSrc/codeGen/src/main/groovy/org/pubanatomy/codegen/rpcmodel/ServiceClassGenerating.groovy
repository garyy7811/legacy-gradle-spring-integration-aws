package com.customshow.codegen.rpcmodel

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * User: GaryY
 * Date: 6/29/2016*/
class ServiceClassGenerating{
    private As3TypeFactory as3TypeFactory = new As3TypeFactory();

    public ServiceClassGenerating( Class<?> javaClass ){
        this.javaClass = javaClass;
    }

    private Class<?> javaClass;

    public String generateCode(){

        Map<String, Method> name2MethodMap = new HashMap<>()


        javaClass.declaredMethods.each {
            if( Modifier.isPublic( it.getModifiers() ) && !Modifier.isStatic( it.getModifiers(  ) ) ){
                //public getter
                if( !( it.name.startsWith( "get" ) && it.parameterCount == 0 ) ){

                    Method prevM = name2MethodMap.get( it.name )
                    if( prevM == null || it.parameterCount > prevM.parameterCount ){
                        name2MethodMap.put( it.name, it );
                    }
                }
            }
        }
        List methodCodeLst = new ArrayList()
        name2MethodMap.values().each {
            methodCodeLst.add( generateMethodCode( it ) )
        }


        def clsName = getClassName()
        String constructor = "    public function " + clsName + "( context:RootContextModule ){\n" +
                "        super( context );\n" + "    }\n\n"

        String importing = "/**\n" + " * Generated by com.customshow.codegen\n" + " *\n" + " */\n" + "package " +
                StringUtils.join( getPackage(), "." ) + "{\n\n" + StringUtils.join( importSets, "\n" ) +
                "\n\n\n//@see " + javaClass.getCanonicalName() + "" + "\npublic class " + clsName +
                " extends RpcService{\n"

        return importing + constructor + methodCodeLst.join( '\r\n\r\n' ) + "\r\n}}"
    }

    private Set<String> importSets = new HashSet<>(
            Arrays.asList( 'import com.flashflexpro.as3lib.rpc.RpcSpringMvcPost;',
                    'import com.flashflexpro.as3lib.rpc.RpcService;',
                    'import com.flashflexpro.as3lib.utils.RootContextModule;' ) );

    public String generateMethodCode( Method method ){
        List psarr = [];

        for( int i = 1; i < method.parameterTypes.length; i++ ){
            def type = as3TypeFactory.getAs3Type( method.parameterTypes[ i ] ).qualifiedName
            psarr.add( method.parameters[ i ].name + ":" + type )
            if( type.indexOf( "." ) > 0 ){
                importSets.add( "import " + type + ";" )
            }
        }

        String palst = ( psarr.size() > 0? ", ": " " ) +psarr.join( ", " )
        return "    public static const RPC_" + method.name + ":String = \"" + method.name + "\";\n" +
                "    public function " + method.name + "( result:Function, fault:Function" + palst +
                " ):RpcSpringMvcPost{\n" + "        return createRpcPost( RPC_" + method.name + ", arguments );\n" +
                "    }";
    }

    public String[] getPackage(){

        String[] split = javaClass.getCanonicalName().split( "\\." );

        return ArrayUtils.remove( split, split.length - 1 )
    }


    public String getClassName(){
        String[] split = javaClass.getCanonicalName().split( "\\." );

        return split[ split.length - 1 ]
    }

}

