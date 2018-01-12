package DBEmulator;

import java.util.ArrayList;

public class PartObject
{
        public String id;
        public String high;
        public String name;
        public String order;
        public String param1;
        
        public UltariLinkedQueue subParts;
        public UltariLinkedQueue subUsers;
        
        public PartObject(String id, String high, String name, String order, String param1)
        {
                this.id = id;
                this.high = high;
                this.name = name;
                this.order = order;
                this.param1 = param1;
                
                subParts = new UltariLinkedQueue();
                subUsers = new UltariLinkedQueue();
        }
}