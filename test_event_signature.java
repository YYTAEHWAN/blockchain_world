import org.web3j.crypto.Hash;

public class test_event_signature {
    public static void main(String[] args) {
        String eventSignature = "PropertyTokenCreated(uint256,address,address,string,string,uint256)";
        String hash = Hash.sha3String(eventSignature);
        System.out.println("Event signature: " + eventSignature);
        System.out.println("Hash: " + hash);
    }
}
