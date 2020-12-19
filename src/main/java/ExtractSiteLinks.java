import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Sites;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;

public class ExtractSiteLinks implements EntityDocumentDumpProcessor{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractSiteLinks.class);
    
    private CSVPrinter printer;
    private Sites sites;
    
    public ExtractSiteLinks(File csvFile, Sites sites){
        try {
            this.printer = CSVFormat.DEFAULT.print(csvFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("Could not write CSV File", ex);
        }
        this.sites = sites;
    }
    
    @Override
    public void open() {}
            
    @Override
    public void processItemDocument(ItemDocument itemDocument) {
        Map<String,SiteLink> map = itemDocument.getSiteLinks();
        if(map == null || map.isEmpty())
            return;
        List<String> urls = new ArrayList(map.size());
        urls.add(itemDocument.getEntityId().getIri()); // add wikidata URI
        for(Entry<String,SiteLink> sitelink : map.entrySet()){            
            String url = this.sites.getSiteLinkUrl(sitelink.getValue());
            if(url == null){
                LOGGER.info("Site key {} is not known", sitelink.getValue());
            }else{
                urls.add(url);
            }
        }
        //if(urls.size() <= 1)
        //    return; // only wikidataURI
        try {
            printer.printRecord(urls);
        } catch (IOException ex) {
            LOGGER.error("Could not write a line in CSV File", ex);
        }
    }
    
    @Override
    public void close() {
        try {
            printer.flush();
            printer.close();
        } catch (IOException ex) {
            LOGGER.error("Could not close CSV File", ex);
        }
    }
    
    
    public static void main(String[] args) throws IOException{
        File csvFile = new File("siteURLs.csv");

        DumpProcessingController dumpProcessingController = new DumpProcessingController("wikidatawiki");
        dumpProcessingController.setOfflineMode(false);

        Sites sites = dumpProcessingController.getSitesInformation();        
        dumpProcessingController.registerEntityDocumentProcessor(new ExtractSiteLinks(csvFile, sites), "", true);

        dumpProcessingController.processMostRecentJsonDump();
    }
    
}
