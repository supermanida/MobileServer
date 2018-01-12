curl -X POST \
-H "Authorization: key= AIzaSyDVPHu-UXZ3gpNfIEl_0bkgb3wKCOVnRjA" \
-H "Content-Type: application/json" \
-d '{ 
"registration_ids": [ 
"APA91bGWChf8H9NhHqV9QveOKos34TPDLTo_tROOijji7VxcISR4y1XM7f-zdvIrwIrGFBMVBjxcgsMbePzBclRd0m7nwW9R0nv7JPl3K_gbPwi2cckFHBUfJwbNFa8McBiVPDqNqS-_"
], 
"data": { 
"message": "Hello Message"
},
"priority": "high"
}' \
https://gcm-http.googleapis.com/gcm/send
