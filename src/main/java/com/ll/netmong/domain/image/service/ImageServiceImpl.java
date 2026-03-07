package com.ll.netmong.domain.image.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.ll.netmong.domain.image.entity.Image;
import com.ll.netmong.domain.image.repository.ImageRepository;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
//    private final AmazonS3Client amazonS3Client;
    private final ImageRepository imageRepository;

//    @Value("${cloud.aws.s3.bucket}")
//    private String bucket;

//    @Value("${cloud.aws.s3.url}")
//    private String bucketUrl;

    @Value("${custom.image.domain}")
    private String imageDomain;

    @Value("${custom.image.url}")
    private String imagePathRoot;

    @Transactional
    public <T> Optional<Image> uploadImage(T requestType, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }

//        String imageLocation = bucketUrl;
//        String imageName = file.getOriginalFilename();

        String originalImageName = file.getOriginalFilename();
        String savedImageName = UUID.randomUUID() + "_" + originalImageName;

        String requestTypeSimpleName = requestType.getClass().getSimpleName() + "/";

        String imagePath = imageDomain + "/" + requestTypeSimpleName + savedImageName;

//        String fileName = requestTypeSimpleName + file.getOriginalFilename();
        String fileName = requestTypeSimpleName + savedImageName;

        Optional<Image> image = Optional.empty();

        if (requestType instanceof Product) {
            Image productImage = Product.createProductImage(fileName, imagePath);
            image = Optional.of(productImage);
        }

        if (requestType instanceof Post) {
            Image postImage = Post.createProductImage(fileName, imagePath);
            image = Optional.of(postImage);
        }

        if (image.isPresent()) {
            imageRepository.save(image.get());
//            createS3Bucket(fileName, file);
            saveImageToLocal(fileName, file);
        }

        return image;
    }

//    private void createS3Bucket(String fileName, MultipartFile image) throws IOException {
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setContentType(image.getContentType());
//        metadata.setContentLength(image.getSize());
//        amazonS3Client.putObject(bucket, fileName, image.getInputStream(), metadata);
//    }

    private void saveImageToLocal(String fileName, MultipartFile image) throws IOException {
        Path savePath = Paths.get(imagePathRoot, fileName).toAbsolutePath().normalize();
        Files.createDirectories(savePath.getParent());
        Files.copy(image.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
    }
}
