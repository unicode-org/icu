public class ListNFComplete {
    
    // find all the characters that are 
    // a) not decomposed by this normalization form
    // b) of combining class 0
    // AND if NKC or NFKC, 
    // c) can never compose with a previous character
    // d) can never compose with a following character
    // e) can never change if another character is added
    //    Example: a-breve might satisfy a-d, but if you
    //    add an ogonek it changes to a-ogonek + breve
        
    public static void main (String[] args) {
        Normalizer nfd = new Normalizer(Normalizer.NFD);
        
    }
}