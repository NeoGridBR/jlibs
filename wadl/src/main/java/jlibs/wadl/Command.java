package jlibs.wadl;

import jlibs.wadl.runtime.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Santhosh Kumar T
 */
public class Command{
    private WADLTerminal terminal;
    public Command(WADLTerminal terminal){
        this.terminal = terminal;
    }

    public void run(String command){
        List<String> args = getArguments(command);

        String arg1 = args.get(0);
        if(arg1.equals("cd")){
            Path path = terminal.getCurrentPath();
            if(args.size()==1)
                path = path.getRoot();
            else
                path = path.get(args.get(1));
            terminal.setCurrentPath(path);
        }
    }
    
    private List<String> getArguments(String command){
        List<String> args = new ArrayList<String>();
        StringTokenizer stok = new StringTokenizer(command, " ");
        while(stok.hasMoreTokens())
            args.add(stok.nextToken());
        return args;
    }
}
