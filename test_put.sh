curl -X PUT http://localhost:8082/api/v1/external-candidates/5 \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"email":"pm1@talentgrid.com","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)" \
-d '{
  "demandId": 5001,
  "firstName": "pavan",
  "lastName": "Kumar",
  "email": "brandnew.email999@gmail.com",
  "phoneNumber": "9111111111",
  "source": "CAREERS_PORTAL",
  "dateOfBirth": "25-08-2000",
  "gender": "Male",
  "address": {
    "street1": "Anna Salai",
    "street2": "Near Metro",
    "city": "Chennai",
    "state": "Tamil Nadu",
    "country": "India",
    "zipCode": "600001"
  },
  "totalExperienceYears": 3.5,
  "skills": [{"skillName": "Java", "yearsOfExperience": 3.5, "proficiency": "ADVANCED"}],
  "educationDetails": [{"degree": "B.Tech", "institution": "IIT Madras", "graduationYear": 2022}]
}'
