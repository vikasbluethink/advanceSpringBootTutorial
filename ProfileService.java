package in.bluethink.products.bpmcore.service;

import in.bluethink.products.bpmcore.data.entity.Profile;
import in.bluethink.products.bpmcore.data.mapper.ProfileMapper;
import in.bluethink.products.bpmcore.data.projection.ProfileProjection;
import in.bluethink.products.bpmcore.data.proxy.DynamicProxy;
import in.bluethink.products.bpmcore.data.repository.ProfileRepository;
import in.bluethink.products.bpmcore.data.request.ProfileCreateRequest;
import in.bluethink.products.bpmcore.exception.ProfileException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ProfileService {

    @Autowired
    ProfileRepository profileRepository;

    public ResponseEntity<List<Profile>> getAll() {
        return new ResponseEntity<>(profileRepository.findAll(), HttpStatus.OK);
    }

    public ResponseEntity<ProfileProjection> getByEmail(String email) throws ProfileException {
        ProfileProjection profile = profileRepository.findByEmail(email, ProfileProjection.class);
        if(profile == null) {
            throw new ProfileException(email+" This email is not exist");
        }else {
            return new ResponseEntity<>(profile, HttpStatus.OK);
        }
    }

    public ProfileProjection createProfile(ProfileCreateRequest createRequest) throws ProfileException {
        Profile candidate = profileRepository.findByEmail(createRequest.getEmail());
        if(candidate != null) {
            throw new ProfileException(createRequest.getEmail()+" This email is already exist");
        }else {
            Profile profile = ProfileMapper.mapToEntity(createRequest, null);
            Profile saved = profileRepository.save(profile);
            return DynamicProxy.getProxy(saved, ProfileProjection.class);
        }
    }

    @Transactional
    public ProfileProjection updateProfile(ProfileCreateRequest createRequest) {
        log.info("Updating profile: {}",createRequest);
        try {
            Profile existingProfile = profileRepository.findByEmail(createRequest.getEmail());
            if(existingProfile != null){
                ProfileMapper.mapToEntity(createRequest,existingProfile);
                existingProfile.mergeProperties(createRequest.getProperties());
                Profile savedProfile = profileRepository.save(existingProfile);
                return DynamicProxy.getProxy(savedProfile, ProfileProjection.class);
            }else {
                log.error("Profile not found for email: {}",createRequest.getEmail());
                throw new RuntimeException("Profile not found for email: "+createRequest.getEmail());
            }
        } catch (Exception ex){
            log.error("Error updating the profile: {}",createRequest);
            throw new RuntimeException("Error updating the profile: {}",ex);
        }
    }

    public List<?> search(String searchTerm) {
        List<Profile> result = new ArrayList<>();
         result =  profileRepository.search(searchTerm);
         if (result.isEmpty()){
             List<String> res = new ArrayList<>();
             res.add("No result found");
             return res;
         }
        return result;
    }

    public Map<String,Long> getByCreatedDateBetweeen(){
        //this year
        LocalDateTime startDate = LocalDateTime.of(2023,01, 01, 00,00,00);
        LocalDateTime endDate = LocalDateTime.of(2023,12, 31, 00,00,00);
        List<Profile> thisYearData = profileRepository.findProfileByCreationDateBetween(startDate, endDate);

        //this Quarter
        LocalDateTime quarterStartDate = LocalDateTime.of(2023,01, 01, 00,00,00);
        LocalDateTime quarterEndDate = LocalDateTime.of(2023,03, 30, 00,00,00);
        List<Profile> thisQuarterData = profileRepository.findProfileByCreationDateBetween(quarterStartDate, quarterEndDate);

        //this Month
        LocalDateTime monthStartDate = LocalDateTime.of(2023,03, 01, 00,00,00);
        LocalDateTime monthEndDate = LocalDateTime.of(2023,03, 30, 00,00,00);
        List<Profile> thisMonthData = profileRepository.findProfileByCreationDateBetween(monthStartDate, monthEndDate);

        Map<String,Long> totalRecord = new HashMap<>();

        totalRecord.put("This Year",  profileRepository.countByYear());
        totalRecord.put("This Quarter", profileRepository.countByQUARTER());
        totalRecord.put("This Month", profileRepository.countByMonth());
        totalRecord.put("Candidate", profileRepository.countCandidate());
        totalRecord.put("Consultant", profileRepository.countConsultant());

        return totalRecord;
    }

}
