UPDATE project_members AS member
SET role = 'OWNER'
FROM projects AS project
WHERE member.project_id = project.id
  AND member.user_id = project.owner_id
  AND member.role <> 'OWNER';
