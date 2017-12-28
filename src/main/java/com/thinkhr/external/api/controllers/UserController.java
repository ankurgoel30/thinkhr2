package com.thinkhr.external.api.controllers;

import static com.thinkhr.external.api.ApplicationConstants.BROKER_ID_PARAM;
import static com.thinkhr.external.api.ApplicationConstants.DEFAULT_BROKER_ID;
import static com.thinkhr.external.api.ApplicationConstants.DEFAULT_SORT_BY_USER_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.hibernate.validator.constraints.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thinkhr.external.api.db.entities.User;
import com.thinkhr.external.api.exception.ApplicationException;
import com.thinkhr.external.api.exception.MessageResourceHandler;
import com.thinkhr.external.api.model.FileImportResult;
import com.thinkhr.external.api.services.UserService;
import com.thinkhr.external.api.services.utils.FileImportUtil;

/**
 * User Controller for performing operations
 * related with User object.
 * 
 */
@RestController
@Validated
@RequestMapping(path="/v1/users")
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;
    
    @Autowired
    MessageResourceHandler resourceHandler;

    /**
     * Get all users from repository
     * @return List<User>
     * 
     */
    @RequestMapping(method=RequestMethod.GET)
    List<User> getAllUser(@Range(min = 0l, message = "Please select positive integer value for 'offset'") 
                @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
                @Range(min = 1l, message = "Please select positive integer and should be greater than 0 for 'limit'")
                @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit, 
                @RequestParam(value = "sort", required = false, defaultValue = DEFAULT_SORT_BY_USER_NAME) String sort,
                @RequestParam(value = "searchSpec", required = false) String searchSpec,
                @RequestParam Map<String, String> allRequestParams) throws ApplicationException {

        return userService.getAllUser(offset, limit, sort, searchSpec, allRequestParams);
    }

    /**
     * Get user with given id from repository
     * @param id user id
     * @return User object
     * @throws ApplicationException 
     * 
     */
    @RequestMapping(method=RequestMethod.GET, value="/{userId}")
    public User getById(@PathVariable(name="userId", value = "userId") Integer userId) throws ApplicationException { 
        return userService.getUser(userId);
    }


    /**
     * Delete specific user from database
     * 
     * @param userId
     */
    @RequestMapping(method=RequestMethod.DELETE, value="/{userId}")
    public ResponseEntity<Integer> deleteUser(@PathVariable(name="userId", value = "userId") Integer userId) throws ApplicationException{
        userService.deleteUser(userId);
        return new ResponseEntity <Integer>(userId, HttpStatus.ACCEPTED);
    }

    /**
     * Update a user in database
     * 
     * @param User object
     */
    @RequestMapping(method=RequestMethod.PUT, value="/{userId}")
    public ResponseEntity <User> updateUser(@PathVariable(name="userId", value = "userId") Integer userId, 
            @Valid @RequestBody User user, @RequestAttribute(name = BROKER_ID_PARAM, required = false) Integer brokerId)
            throws ApplicationException {
        if (brokerId == null) {
            brokerId = DEFAULT_BROKER_ID;
        }
        user.setUserId(userId);
        userService.updateUser(user, brokerId);
        return new ResponseEntity<User> (user, HttpStatus.OK);
    }


    /**
     * Add a user in database
     * 
     * @param User object
     */
    @RequestMapping(method=RequestMethod.POST)
    public ResponseEntity<User> addUser(@Valid @RequestBody User user,
            @RequestAttribute(name = BROKER_ID_PARAM, required = false) Integer brokerId) throws ApplicationException {
        if (brokerId == null) {
            brokerId = DEFAULT_BROKER_ID;
        }
        userService.addUser(user, brokerId);
        return new ResponseEntity<User>(user, HttpStatus.CREATED);
    }
    
    

    /**
     * Bulk import company records from a given CSV file.
     * 
     * @param Multipart file CSV files with records
     * @param brokerId - brokerId from request. Originally retrieved as part of JWT token
     * 
     */
    @RequestMapping(method=RequestMethod.POST,  value="/bulk")
    public ResponseEntity <InputStreamResource> bulkUploadFile(@RequestParam(value="file", required=false) MultipartFile file, 
            @RequestAttribute(name = BROKER_ID_PARAM, required = false) Integer brokerId)
                    throws ApplicationException, IOException {

        logger.info("##### ######### USER IMPORT BEGINS ######### #####");
        if (brokerId == null) {
            brokerId = DEFAULT_BROKER_ID;
        }
        FileImportResult fileImportResult = userService.bulkUpload(file, brokerId);
        logger.debug("************** USER IMPORT ENDS *****************");

        // Set the attachment header & set up response to return a CSV file with result and erroneous records
        // This response CSV file can be used by users to resubmit records after fixing them.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-disposition", "attachment;filename=companiesImportResult.csv");

        File responseFile = FileImportUtil.createReponseFile(fileImportResult, resourceHandler);

        return ResponseEntity.ok().headers(headers).contentLength(responseFile.length()).contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(new FileInputStream(responseFile)));
    }

}

