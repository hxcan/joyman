    private void initViews() {
        Log.d(TAG, "initViews: Initializing views");
        textTitle = findViewById(R.id.text_detail_title);
        textDescription = findViewById(R.id.text_detail_description);
        textStatus = findViewById(R.id.text_detail_status);
        textPriority = findViewById(R.id.text_detail_priority);
        textProject = findViewById(R.id.text_detail_project);
        textCreatedAt = findViewById(R.id.text_detail_created_at);
        spinnerProject = findViewById(R.id.spinner_detail_project);
        
        findViewById(R.id.btn_toggle_status).setOnClickListener(v -> toggleStatus());
        findViewById(R.id.btn_move_project).setOnClickListener(v -> showMoveProjectDialog());
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteConfirm());
        findViewById(R.id.btn_save_project).setOnClickListener(v -> saveProjectSelection());
        
        Log.d(TAG, "initViews: Views initialized and listeners set");
    }