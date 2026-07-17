ALTER TABLE sync_job_run
  ADD COLUMN IF NOT EXISTS source_watermark_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_sync_job_run_one_running_source
  ON sync_job_run (source_type)
  WHERE status = 'running';
