package CombiEngine.Mobile;

import java.util.Arrays;

public class AriaWrapper {
        private final static String TAG = "AriaWrapper";

        //2015-10-20
        public static String encryptSkeyWrapper(final String token, final String value)
        {
                try
                {
                        String data = "";
                        String key = token;
                        
                        byte[] by = key.getBytes();
                        byte[] arr = new byte[256];
                        
                        Arrays.fill( arr, ( byte)0);
                        
                        for ( int i = 0; i < by.length; i++ )
                                arr[i] = by[i];
                        
                        data = AriaAlgorithm.encrypt( value, arr, 256, null );
                        
                        return data;
                }
                catch ( Exception e )
                {
                        e.printStackTrace();
                }
                return null;
        }
        
        //2015-10-20
        public static String decryptSkeyWrapper(final String token, final String value)
        {
                try
                {
                        String data = "";
                        String key = token;
                        
                        byte[] by = key.getBytes();
                        byte[] arr = new byte[256];
                        
                        Arrays.fill( arr, ( byte)0);
                        
                        for ( int i = 0; i < by.length; i++ )
                                arr[i] = by[i];
                        
                        data = AriaAlgorithm.decrypt( value, arr, 256, null );
                        
                        return data;
                }
                catch ( Exception e )
                {
                        e.printStackTrace();
                }
                return null;
        }
        
}
