package com.johnson.contactapi.service;

import com.johnson.contactapi.domain.Contact;
import com.johnson.contactapi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactService {
    private final ContactRepo contactRepo;

    public Page<Contact> getAllContacts(int page, int size){
        return contactRepo.findAll(PageRequest.of(page,size, Sort.by("name")));
    }

    public Contact getContact(String id){
        return contactRepo.findById(id).orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public Contact createContact(Contact contact){
        return contactRepo.save(contact);
    }

    public void deleteContact(Contact contact){

    }

    public String uploadPhoto(String id, MultipartFile file){
        Contact contact = getContact(id);
        String photoUrl= photoFunction.apply(id,file);
        contact.setPhotoUrl(photoUrl);
        contactRepo.save(contact);
        return photoUrl;
    }

    //this will be returning an optional of "filename" and will be filtered to make sure it contains a "."
    //if there is a "." we need what comes after it. (that's the +1s purpose) essentially grabbing the "png" part of ".png"
    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");


    //this method will be taking in a String (id), a MultipartFile (image), and then returning a string.
    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get("").toAbsolutePath().normalize();

            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);

            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image" + filename).toUriString();

        }catch(Exception e) {
            throw new RuntimeException("Unable to save image");
        }
    };
}
