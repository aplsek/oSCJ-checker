#!/usr/bin/env ruby

header_skip=ARGV.shift.to_i
underscore=ARGV.shift

cnt=0

while (line=gets)
  if line =~ /# begin barrier/
    puts "L_OvmBarrierProf_Begin_#{cnt}:"
    while (line=gets)
      if line =~ /# begin barrier/
        $stderr.puts "Dude, I just saw a 'begin barrier' whie looking for an 'end barrier'"
      elsif line =~ /# end barrier/
        puts "L_OvmBarrierProf_End_#{cnt}:"
        cnt+=1
        break
      else
        puts line
      end
    end
  elsif line =~ /# end barrier/
    $stderr.puts "Dude, I just saw an 'end barrier' while looking for a 'begin barrier'"
  else
    puts line
  end
end

puts "\t.data"
puts "\t.align 8"
puts "\t.globl #{underscore}barrier_prof_ranges"
puts "#{underscore}barrier_prof_ranges:"
(header_skip/4).times {
  puts "\t.long 0"
}
puts "\t.long #{cnt}"
cnt.times {
  | i |
  puts "\t.long L_OvmBarrierProf_Begin_#{i}"
  puts "\t.long L_OvmBarrierProf_End_#{i}"
}



