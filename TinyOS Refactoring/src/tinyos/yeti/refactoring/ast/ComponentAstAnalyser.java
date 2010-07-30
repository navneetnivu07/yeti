package tinyos.yeti.refactoring.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import tinyos.yeti.nesc12.parser.ast.nodes.AbstractFixedASTNode;
import tinyos.yeti.nesc12.parser.ast.nodes.definition.TranslationUnit;
import tinyos.yeti.nesc12.parser.ast.nodes.general.Identifier;
import tinyos.yeti.nesc12.parser.ast.nodes.nesc.Access;
import tinyos.yeti.nesc12.parser.ast.nodes.nesc.AccessList;
import tinyos.yeti.nesc12.parser.ast.nodes.nesc.InterfaceReference;
import tinyos.yeti.nesc12.parser.ast.nodes.nesc.InterfaceType;
import tinyos.yeti.nesc12.parser.ast.nodes.nesc.ParameterizedInterface;
import tinyos.yeti.nesc12.parser.ast.nodes.nesc.ParameterizedInterfaceList;
import tinyos.yeti.refactoring.utilities.ASTUtil;

public class ComponentAstAnalyser extends AstAnalyzer {
	
	protected TranslationUnit root;
	protected Identifier componentIdentifier;
	protected AccessList specification;
	
	private Collection<InterfaceReference> interfaceReferences;
	private Collection<Identifier> referencedInterfaceIdentifiers;
	private Collection<Identifier> referencedInterfaceAliasIdentifiers;
	private Map<Identifier,Identifier> alias2AliasedInterface;
	
	public ComponentAstAnalyser(TranslationUnit root,Identifier componentIdentifier, AccessList specification) {
		super();
		this.root = root;
		this.componentIdentifier = componentIdentifier;
		this.specification = specification;
	}
	
	/**
	 * Returns the name identifier of this component.
	 * @return
	 */
	public Identifier getComponentIdentifier(){
		return componentIdentifier;
	}
	
	/**
	 * Returns the name of this component.
	 * @return
	 */
	public String getComponentName(){
		return componentIdentifier.getName();
	}
	
	/**
	 * Gathers all interfaces which are referenced in the specification of this NesC component.
	 * @return
	 */
	public Collection<InterfaceReference> getInterfaceReferences(){
		if(interfaceReferences==null){
			ASTUtil astUtil=getASTUtil();
			Collection<Access> accesses=astUtil.getChildsOfType(specification, Access.class);
			Collection<ParameterizedInterfaceList> interfaceLists=collectFieldsWithName(accesses, Access.INTERFACES);
			Collection<ParameterizedInterface> parametrizedInterfaces=new LinkedList<ParameterizedInterface>();
			for(ParameterizedInterfaceList list: interfaceLists){
				parametrizedInterfaces.addAll(astUtil.getChildsOfType(list, ParameterizedInterface.class));
			}
			interfaceReferences=collectFieldsWithName(parametrizedInterfaces,ParameterizedInterface.REFERENCE);
		}
		return interfaceReferences;
	}
	
	/**
	 * Gathers all interface identifiers of the interfaces which are referenced in the specification of this NesC component.
	 * @return
	 */
	public Collection<Identifier> getReferencedInterfaceIdentifiers(){
		if(referencedInterfaceIdentifiers==null){
			Collection<InterfaceType> interfaceTypes=collectFieldsWithName(getInterfaceReferences(),InterfaceReference.NAME);
			referencedInterfaceIdentifiers=collectFieldsWithName(interfaceTypes, InterfaceType.NAME);
		}
		return referencedInterfaceIdentifiers;
	}
	
	/**
	 * Gathers all interface alias identifiers of the interfaces which are referenced in the specification of this NesC component and are aliased.
	 * @return
	 */
	public Collection<Identifier> getReferencedInterfaceAliasIdentifiers(){
		if(referencedInterfaceAliasIdentifiers==null){
			referencedInterfaceAliasIdentifiers=collectFieldsWithName(getInterfaceReferences(), InterfaceReference.RENAME);
		}
		return referencedInterfaceAliasIdentifiers;
	}

	/**
	 * Returns a map which maps an interface alias identifier to the identifier of the interface it aliases, in the specification of a NesC Component.
	 * @return
	 */
	public Map<Identifier,Identifier> getAlias2AliasedInterface(){
		if(alias2AliasedInterface==null){
			alias2AliasedInterface=new HashMap<Identifier, Identifier>();
			for(InterfaceReference reference:getInterfaceReferences()){
				Identifier aliasIdentifier=(Identifier)reference.getField(InterfaceReference.RENAME);
				if(aliasIdentifier!=null){
					InterfaceType type=(InterfaceType)reference.getField(InterfaceReference.NAME);
					if(type!=null){
						Identifier interfaceIdentifier=(Identifier)type.getField(InterfaceType.NAME);
						if(interfaceIdentifier!=null){
							alias2AliasedInterface.put(aliasIdentifier, interfaceIdentifier);
						}
					}
				}
			}
		}
		return alias2AliasedInterface;
	}
	
	/**
	 * Returns the identifier of the interface which is aliased with the given alias, in the specification of a NesC Component.
	 * Use {@link tinyos.yeti.refactoring.ast.AstAnalyzer#getAliasIdentifier4InterfaceAliasName(String alias) getIdentifierForInterfaceAliasName} to get the alias identifier.
	 * @param alias
	 * @return
	 */
	public Identifier getInerfaceIdentifier4InterfaceAliasIdentifier(Identifier alias){
		return getAlias2AliasedInterface().get(alias);
	}
	
	/**
	 * Returns the Identifier of the interface alias with the given name in the specification of a NesC Component.
	 * Returns null if there is no alias with the given name.
	 * @param alias
	 * @return
	 */
	public Identifier getAliasIdentifier4InterfaceAliasName(String alias){
		for(Identifier identifier:getReferencedInterfaceAliasIdentifiers()){
			if(alias.equals(identifier.getName())){
				return identifier;
			}
		}
		return null;
	}
	
	/**
	 * Returns the identifier of the interface with the given name in the specification of a NesC Component.
	 * Returns null if there is no alias with the given name.
	 * @param alias
	 * @return
	 */
	public Identifier getInterfaceIdentifier4InterfaceAliasName(String alias){
		Identifier aliasIdentifier= getAliasIdentifier4InterfaceAliasName(alias);
		if(aliasIdentifier!=null){
			return getInerfaceIdentifier4InterfaceAliasIdentifier(aliasIdentifier);
		}
		return null;
	}
	
	/**
	 * Returns the name of the interface with the given name in the specification of a NesC Component.
	 * Returns null if there is no alias with the given name.
	 * @param alias
	 * @return
	 */
	public String getInterfaceName4InterfaceAliasName(String alias){
		Identifier interfaceIdentifier= getInterfaceIdentifier4InterfaceAliasName(alias);
		if(interfaceIdentifier!=null){
			return interfaceIdentifier.getName();
		}
		return null;
	}
	
	/**
	 * Checks if the given name is actually an alias, which is a rename with the NesC "as" keyword, of an interface in the specification of a NesC component.
	 * @param name
	 * @return
	 */
	public boolean isDefinedInterfaceAliasName(String name){
		Identifier alisaIdentifier=getAliasIdentifier4InterfaceAliasName(name);
		return alisaIdentifier!=null;
	}
	
	
	/**
	 * Collects of every given parent the field with the fieldName and adds it to the returned collection, if it is not null.
	 * @param <CHILD_TYPE>	The type which the field with the given fielName has.
	 * @param parents	The AbstractFixedASTNodes of which we want to collect a field/child. 
	 * @param fieldName The name of the field we are interested in.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <CHILD_TYPE> Collection<CHILD_TYPE> collectFieldsWithName(Collection<? extends AbstractFixedASTNode> parents,String fieldName){
		Collection<CHILD_TYPE> childs=new LinkedList<CHILD_TYPE>();
		for(AbstractFixedASTNode parent:parents){
			CHILD_TYPE child=(CHILD_TYPE)parent.getField(fieldName);
			if(child!=null){
				childs.add(child);
			}
		}
		return childs;
	}
}

	