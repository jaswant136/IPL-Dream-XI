import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

export const registerSchema = z
  .object({
    displayName: z.string().trim().min(2, 'At least 2 characters').max(40, 'At most 40 characters'),
    email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    password: z.string().min(8, 'Use at least 8 characters'),
    confirm: z.string().min(1, 'Confirm your password'),
  })
  .refine((v) => v.password === v.confirm, { path: ['confirm'], message: 'Passwords do not match' })

export const resetSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
})

export const profileSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, 'At least 2 characters')
    .max(40, 'At most 40 characters')
    .regex(/^[\p{L}\p{N} _.'-]+$/u, 'Letters, numbers, spaces and _ . \' - only'),
})
