package org.civicfix.app.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");


    private Path storageLocation;

    @PostConstruct
    public void init(){
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try{
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare la cartella di upload", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Il file è vuoto");
        }

        String extension = getExtension(file.getOriginalFilename()).toLowerCase();
        boolean validExtension = ALLOWED_EXTENSIONS.contains(extension);
        boolean validContentType = file.getContentType() != null && ALLOWED_TYPES.contains(file.getContentType());

        if (!validExtension && !validContentType) {
            throw new IllegalArgumentException("Formato non supportato: solo JPEG, PNG o WEBP");
        }

        String filename = UUID.randomUUID() + extension;

        try {
            Path target = storageLocation.resolve(filename);
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new RuntimeException("Errore nel salvataggio del file", e);
        }

        return filename;
    }

    /**
     * Rimuove un file caricato. Un errore non viene propagato: la cancellazione
     * della segnalazione a cui il file apparteneva è già avvenuta, e lasciare
     * un file orfano è meno grave che far fallire l'operazione a metà.
     *
     * <p>Il nome viene ridotto al solo ultimo segmento e si verifica che il
     * percorso risolto resti dentro la cartella di upload: un valore come
     * "../../qualcosa" non deve poter uscire da lì.
     */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        try {
            Path target = storageLocation.resolve(Paths.get(filename).getFileName()).normalize();
            if (!target.startsWith(storageLocation)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Nessuna propagazione: si veda il commento sopra.
        }
    }

    private String getExtension(String originalFilename){
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
