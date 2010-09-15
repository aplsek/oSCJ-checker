package checkers;

import javax.lang.model.element.Element;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

public class MyUtils {

    public static final Element elementFromUse(ExpressionTree node) {
        
        System.out.println("vvv" + ((NewArrayTree) node).getKind());
        Tree nodeTypeTree2 = ((ArrayTypeTree) node).getType();
        Element var2 = TreeInfo.symbol((JCTree)nodeTypeTree2);
        System.out.println("var " + var2);
        
        switch (node.getKind()) {
        case IDENTIFIER:
        case MEMBER_SELECT:
        case METHOD_INVOCATION:
            return TreeInfo.symbol((JCTree)node);
        case ARRAY_ACCESS:  
            System.out.println("vvv" + ((NewArrayTree) node).getKind());
            Tree nodeTypeTree = ((ArrayTypeTree) node).getType();
            Element var = TreeInfo.symbol((JCTree)nodeTypeTree);
            System.out.println("var " + var);
            return var;
        default:
            throw new IllegalArgumentException("Tree not use: " + node.getKind());
        }
    }
    
}
