### Step 2: Auth

🔹 **EnumType.STRING**  
→ Avoid silent bugs when reordering enums  
→ Particularly dangerous with roles/permissions

🔹 **Do not use @Data for entities**
→ Incorrect `equals/hashCode`  
→ `toString` causes lazy loading  
→ Entity is not a DTO  

🔹 **Instant** for created date:  
→ Instant = absolute time  
→ No time zone restrictions  
→ No time discrepancies between server and database  
→ Ideal for entity and audit fields 

🔹 **@Autowired**  
→ **Spring 4.3+** automatically injects a single constructor.  
→ `@Autowired` is optional in this case.  
→ Constructor injection is best practice.  
→ It's not missing, it's written correctly.  