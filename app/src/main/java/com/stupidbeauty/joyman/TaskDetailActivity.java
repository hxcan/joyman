    private void updateUI() {
        Log.d(TAG, "updateUI: START - Updating UI");
        if (task == null) {
            Log.w(TAG, "updateUI: Task is null, skipping update");
            return;
        }
        
        textTitle.setText(task.getTitle());
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            textDescription.setText(task.getDescription());
            textDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            textDescription.setVisibility(android.view.View.GONE);
        }
        
        textStatus.setText("状态：" + task.getStatusText());
        textPriority.setText("优先级：" + task.getPriorityText());
        
        // 显示所属项目
        Long projectId = task.getProjectId();
        Log.i(TAG, "updateUI: Task project ID: " + (projectId == null ? "null" : projectId));
        
        if (projectId != null) {
            Log.i(TAG, "updateUI: Observing projects to find project ID: " + projectId);
            projectViewModel.getAllProjects().observe(this, projects -> {
                Log.i(TAG, "updateUI: Projects observer triggered, projects count: " + (projects == null ? "null" : projects.size()));
                
                boolean found = false;
                if (projects != null) {
                    for (Project p : projects) {
                        Log.d(TAG, "updateUI: Checking project: " + p.getId() + " - " + p.getName());
                        if (p.getId() == projectId) {
                            String projectDisplay = "所属项目：" + p.getIconDisplay() + " " + p.getName();
                            textProject.setText(projectDisplay);
                            Log.i(TAG, "updateUI: Project found! Display: " + projectDisplay);
                            found = true;
                            break;
                        }
                    }
                }
                
                if (!found) {
                    Log.w(TAG, "updateUI: Project not found! ID: " + projectId);
                    textProject.setText("所属项目：未知项目 (ID: " + projectId + ")");
                }
            });
        } else {
            Log.i(TAG, "updateUI: Task has no project");
            textProject.setText("所属项目：无");
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textCreatedAt.setText("创建时间：" + sdf.format(new Date(task.getCreatedAt())));
        
        Log.d(TAG, "updateUI: END - UI updated");
    }