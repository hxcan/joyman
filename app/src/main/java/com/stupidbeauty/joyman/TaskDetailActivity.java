    private void loadProjects() {
        Log.d(TAG, "loadProjects: START - Loading projects for spinner");
        Log.d(TAG, "loadProjects: Activity is destroyed: " + isDestroyed());
        Log.d(TAG, "loadProjects: Activity is finishing: " + isFinishing());
        
        projectViewModel.getAllProjects().observe(this, projects -> {
            Log.d(TAG, "loadProjects: Observer triggered, projects is null: " + (projects == null));
            Log.d(TAG, "loadProjects: Activity is destroyed: " + isDestroyed());
            
            if (isDestroyed()) {
                Log.w(TAG, "loadProjects: Activity already destroyed, skipping update");
                return;
            }
            
            projectList = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            
            // 添加"无项目"选项
            projectList.add(null);
            projectNames.add("无项目");
            Log.d(TAG, "loadProjects: Added 'no project' option");
            
            // 添加所有项目
            if (projects != null) {
                for (Project project : projects) {
                    projectList.add(project);
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                }
                Log.d(TAG, "loadProjects: Added " + projects.size() + " projects");
            } else {
                Log.w(TAG, "loadProjects: Projects list is null");
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProject.setAdapter(adapter);
            Log.d(TAG, "loadProjects: Adapter created and set");
            
            // 设置当前选中的项目
            if (task != null && task.getProjectId() != null) {
                Log.d(TAG, "loadProjects: Setting selection for task with projectId: " + task.getProjectId());
                for (int i = 0; i < projectList.size(); i++) {
                    Project p = projectList.get(i);
                    if (p != null && p.getId() == task.getProjectId()) {
                        spinnerProject.setSelection(i);
                        Log.d(TAG, "loadProjects: Selected index: " + i);
                        break;
                    }
                }
            } else {
                Log.d(TAG, "loadProjects: Task has no project, selecting 'no project'");
                spinnerProject.setSelection(0);
            }
            
            // Spinner 不再自动保存，只用于显示和选择
            Log.d(TAG, "loadProjects: Listener set up (no auto-save)");
            Log.d(TAG, "loadProjects: END");
        });
    }
    
    /**
     * 保存项目选择更改
     */
    private void saveProjectSelection() {
        Log.d(TAG, "saveProjectSelection: START");
        
        if (task == null) {
            Log.w(TAG, "saveProjectSelection: Task is null");
            Toast.makeText(this, "任务未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int selectedPosition = spinnerProject.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= projectList.size()) {
            Log.e(TAG, "saveProjectSelection: Invalid position: " + selectedPosition);
            Toast.makeText(this, "选择无效", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Project selectedProject = projectList.get(selectedPosition);
        Long currentProjectId = task.getProjectId();
        Long newProjectId = (selectedProject == null) ? null : selectedProject.getId();
        
        Log.i(TAG, "saveProjectSelection: Current projectId: " + currentProjectId + ", New projectId: " + newProjectId);
        
        // 检查是否有变化
        if ((currentProjectId == null && newProjectId == null) || 
            (currentProjectId != null && currentProjectId.equals(newProjectId))) {
            Log.d(TAG, "saveProjectSelection: No change needed");
            Toast.makeText(this, "项目未变更", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 执行保存
        task.setProjectId(newProjectId);
        Log.i(TAG, "saveProjectSelection: Saving task with projectId: " + newProjectId);
        taskViewModel.update(task);
        
        String projectName = (selectedProject == null) ? "无项目" : (selectedProject.getIconDisplay() + " " + selectedProject.getName());
        Toast.makeText(this, "已保存到：" + projectName, Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "saveProjectSelection: END");
        updateUI();
    }